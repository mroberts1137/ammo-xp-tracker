package com.cosmos;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.api.Skill;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/**
 * A RuneLite plugin that tracks experience gained per ammunition used.
 * This plugin monitors the ammunition slot in the equipment panel and tracks
 * combat experience gained to calculate efficiency metrics.
 */
@Slf4j
@PluginDescriptor(
		name = "Ammo XP Tracker",
		description = "Tracks XP gained per ammo used",
		tags = {"ammo", "xp", "tracker", "ranged", "efficiency"}
)
public class AmmoXpTrackerPlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private AmmoXpTrackerConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	private AmmoDebugOverlay debugOverlay;

	private NavigationButton navButton;
	private AmmoXpTrackerPanel panel;

	@Getter
	private boolean tracking = false;
	@Getter
	private int initialAmmoCount = 0;
	@Getter
	private int currentAmmoCount = 0;
	@Getter
	private int ammoUsed = 0;
	@Getter
	private int totalXpGained = 0;
	@Getter
	private float avgXpPerAmmo = 0.0f;
	@Getter
	private String currentAmmoName = "None";

	// XP tracking maps
	private final Map<Skill, Integer> initialSkillXp = new EnumMap<>(Skill.class);
	private final Map<Skill, Integer> currentSkillXp = new EnumMap<>(Skill.class);

	@Override
	protected void startUp() throws Exception {
		debugLog("Ammo XP Tracker starting up");
		panel = new AmmoXpTrackerPanel(this);

		debugOverlay = new AmmoDebugOverlay(this);
		overlayManager.add(debugOverlay);

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");
		navButton = NavigationButton.builder()
				.tooltip("Ammo XP Tracker")
				.icon(icon)
				.priority(5)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown() throws Exception {
		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(debugOverlay);
		resetTracking();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Logged in", null);
			startTracking();
		}
	}

	/**
	 * Handles experience changes in combat skills.
	 * Updates the tracking statistics when relevant skills gain experience.
	 *
	 * @param event The StatChanged event containing skill and experience information
	 */
	@Subscribe
	public void onStatChanged(StatChanged event) {
		if (!tracking) {
			debugLog("Stat changed but tracking is disabled");
			return;
		}

		Skill skill = event.getSkill();
		if (skill == Skill.RANGED || skill == Skill.MAGIC || skill == Skill.ATTACK ||
				skill == Skill.STRENGTH || skill == Skill.DEFENCE) {
			debugLog(String.format("Combat skill %s changed, XP: %d", skill.getName(), event.getXp()));
			updateSkillXp(skill, event.getXp());
			calculateTotalXpGained();
			updateStats();
		}
	}

	/**
	 * Updates ammunition count and statistics on each game tick.
	 *
	 * @param event The GameTick event
	 */
	@Subscribe
	public void onGameTick(GameTick event) {
		if (!tracking) {
			return;
		}

		updateAmmoCount();
		updateStats();
	}

	/**
	 * Starts tracking ammunition usage and experience gain.
	 * Initializes all tracking variables and begins monitoring equipment and experience.
	 */
	public void startTracking() {
		System.out.println("Debug: Attempting to start tracking");
		debugLog("Attempting to start tracking");

		if (tracking) {
			debugLog("Already tracking, ignoring start request");
			return;
		}

		if (client == null) {
			log.error("Client is null, cannot start tracking");
			return;
		}

		// Initialize XP tracking for combat skills
		initialSkillXp.clear();
		currentSkillXp.clear();

		// Log player state
		debugLog(String.format("Player logged in: %B", client.getGameState() == GameState.LOGGED_IN));

		// Initialize combat skill XP tracking
		for (Skill skill : Skill.values()) {
			if (skill == Skill.RANGED || skill == Skill.MAGIC || skill == Skill.ATTACK ||
					skill == Skill.STRENGTH || skill == Skill.DEFENCE) {
				int xp = client.getSkillExperience(skill);
				initialSkillXp.put(skill, xp);
				currentSkillXp.put(skill, xp);
				debugLog(String.format("Initial %s XP: %d", skill.getName(), xp));
			}
		}

		// Get initial ammo count
		updateAmmoCount();
		initialAmmoCount = currentAmmoCount;
		ammoUsed = 0;
		totalXpGained = 0;
		avgXpPerAmmo = 0;

		tracking = true;
		updateStats();
	}

	/**
	 * Resets all tracking variables and stops monitoring.
	 */
	public void resetTracking() {
		debugLog("Resetting tracking");
		tracking = false;
		initialAmmoCount = 0;
		currentAmmoCount = 0;
		ammoUsed = 0;
		totalXpGained = 0;
		avgXpPerAmmo = 0;

		initialSkillXp.clear();
		currentSkillXp.clear();

		updateStats();
	}

	/**
	 * Updates the current ammunition count by checking the equipment ammunition slot.
	 */
	private void updateAmmoCount() {
		debugLog("Updating ammo count...");
		Item ammoItem = client.getItemContainer(InventoryID.EQUIPMENT).getItem(EquipmentInventorySlot.AMMO.getSlotIdx());
		if (ammoItem != null)
		{
			initialAmmoCount = ammoItem.getQuantity();
			currentAmmoCount = ammoItem.getQuantity();;
			currentAmmoName = client.getItemDefinition(ammoItem.getId()).getName();
			debugLog(String.format("Current ammo: %s (%d)", currentAmmoName, currentAmmoCount));
		}
		debugLog("Ammo slot is empty");

		// Calculate ammo used
		ammoUsed = initialAmmoCount - currentAmmoCount;
		if (ammoUsed < 0) {
			// Player might have added ammo, reset counter
			initialAmmoCount = currentAmmoCount;
			ammoUsed = 0;
		}
	}

	private void updateSkillXp(Skill skill, int xp) {
		currentSkillXp.put(skill, xp);
	}

	private void calculateTotalXpGained() {
		totalXpGained = 0;

		for (Skill skill : initialSkillXp.keySet()) {
			int initial = initialSkillXp.get(skill);
			int current = currentSkillXp.get(skill);
			totalXpGained += (current - initial);
		}
	}

	/**
	 * Updates all tracking statistics and refreshes the panel display.
	 */
	private void updateStats() {
		// Calculate average XP per ammo
		if (ammoUsed > 0) {
			avgXpPerAmmo = (float) totalXpGained / ammoUsed;
			debugLog(String.format("Stats updated - XP: %d, Ammo used: %d, Avg XP/ammo: %.2f",
					totalXpGained, ammoUsed, avgXpPerAmmo));
		} else {
			avgXpPerAmmo = 0;
		}

		// Update the panel
		panel.update();
	}

	private void debugLog(String message) {
		if (panel != null) {
			panel.logDebug(message);
		}
//		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
		log.debug(message); // Still log normally too
	}
	
	@Provides
	AmmoXpTrackerConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(AmmoXpTrackerConfig.class);
	}
}