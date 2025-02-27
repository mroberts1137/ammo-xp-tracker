package com.cosmos;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
//import net.runelite.api.events.ExperienceChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.Skill;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

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

	private final Map<Skill, Integer> initialSkillXp = new EnumMap<>(Skill.class);
	private final Map<Skill, Integer> currentSkillXp = new EnumMap<>(Skill.class);

	@Override
	protected void startUp() throws Exception {
		panel = new AmmoXpTrackerPanel(this);

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
		resetTracking();
	}

	@Subscribe
	public void onStatChanged(StatChanged event) {
		if (!tracking) {
			return;
		}

		Skill skill = event.getSkill();
		if (skill == Skill.RANGED || skill == Skill.MAGIC || skill == Skill.ATTACK ||
				skill == Skill.STRENGTH || skill == Skill.DEFENCE) {
			updateSkillXp(skill, event.getXp());
			calculateTotalXpGained();
			updateStats();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if (!tracking) {
			return;
		}

		updateAmmoCount();
		updateStats();
	}

//	@Subscribe
//	public void onExperienceChanged(ExperienceChanged event)
//	{
//		if (!isTracking)
//		{
//			return;
//		}
//
//		// Update XP gained
//		if (event.getSkill() == Skill.RANGED)
//		{
//			int newXp = client.getSkillExperience(Skill.RANGED);
//			int oldXp = newXp - event.getXp();
//			totalXpGained += (newXp - oldXp);
//		}
//
//		// Update current ammo count
//		Item ammoItem = client.getItemContainer(InventoryID.EQUIPMENT).getItem(EquipmentInventorySlot.AMMO.getSlotIdx());
//		if (ammoItem != null)
//		{
//			currentAmmoCount = ammoItem.getQuantity();
//		}
//	}

	public void startTracking() {
		if (tracking) {
			return;
		}

		// Initialize XP tracking for combat skills
		initialSkillXp.clear();
		currentSkillXp.clear();

		for (Skill skill : Skill.values()) {
			if (skill == Skill.RANGED || skill == Skill.MAGIC || skill == Skill.ATTACK ||
					skill == Skill.STRENGTH || skill == Skill.DEFENCE) {
				int xp = client.getSkillExperience(skill);
				initialSkillXp.put(skill, xp);
				currentSkillXp.put(skill, xp);
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

	public void resetTracking() {
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

	private void updateAmmoCount() {
//		Widget equipmentContainer = client.getWidget(WidgetInfo.EQUIPMENT_INVENTORY_ITEMS_CONTAINER);

		currentAmmoCount = 0;
		currentAmmoName = "None";

		Item ammoItem = client.getItemContainer(InventoryID.EQUIPMENT).getItem(EquipmentInventorySlot.AMMO.getSlotIdx());
		if (ammoItem != null)
		{
			initialAmmoCount = ammoItem.getQuantity();
			currentAmmoCount = ammoItem.getQuantity();;
			currentAmmoName = client.getItemDefinition(ammoItem.getId()).getName();
			totalXpGained = 0;
		}

//		if (equipmentContainer != null && !equipmentContainer.isHidden()) {
//			Widget[] ammoSlot = equipmentContainer.getDynamicChildren();
//
//			if (ammoSlot.length > 0) {
//				Widget ammo = ammoSlot[0];
//				if (ammo != null && ammo.getItemId() > 0) {
//					currentAmmoCount = ammo.getItemQuantity();
//					ItemComposition ammoItem = client.getItemDefinition(ammo.getItemId());
//					currentAmmoName = ammoItem.getName();
//				}
//			}
//		}

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

	private void updateStats() {
		// Calculate average XP per ammo
		if (ammoUsed > 0) {
			avgXpPerAmmo = (float) totalXpGained / ammoUsed;
		} else {
			avgXpPerAmmo = 0;
		}

		// Update the panel
		panel.update();
	}

	@Provides
	AmmoXpTrackerConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(AmmoXpTrackerConfig.class);
	}
}