package com.cosmos;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class AmmoDebugOverlay extends Overlay {
    private final AmmoXpTrackerPlugin plugin;

    @Inject
    private Client client;

    public AmmoDebugOverlay(AmmoXpTrackerPlugin plugin) {
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
        setPriority(OverlayPriority.LOW);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return null;
        }

        List<String> debugInfo = new ArrayList<>();
        debugInfo.add("Tracking: " + plugin.isTracking());
        debugInfo.add("Ammo: " + plugin.getCurrentAmmoName() + " (" + plugin.getCurrentAmmoCount() + ")");
        debugInfo.add("Initial: " + plugin.getInitialAmmoCount());
        debugInfo.add("Used: " + plugin.getAmmoUsed());
        debugInfo.add("XP: " + plugin.getTotalXpGained());

        int y = 40;
        for (String line : debugInfo) {
            graphics.setColor(Color.BLACK);
            graphics.drawString(line, 11, y + 1);
            graphics.setColor(Color.WHITE);
            graphics.drawString(line, 10, y);
            y += 15;
        }

        return new Dimension(150, y);
    }
}