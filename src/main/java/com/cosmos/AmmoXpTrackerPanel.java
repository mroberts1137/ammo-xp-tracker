package com.cosmos;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.ColorJButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.DecimalFormat;

@Slf4j
public class AmmoXpTrackerPanel extends PluginPanel {
    private final AmmoXpTrackerPlugin plugin;

    private JLabel statusLabel;
    private JLabel ammoTypeLabel = new JLabel();
    private JLabel ammoUsedLabel = new JLabel();
    private JLabel totalXpGainedLabel = new JLabel();
    private JLabel avgXpPerAmmoLabel = new JLabel();
    private JButton trackButton;
    private JButton resetButton;

    private final DecimalFormat formatter = new DecimalFormat("#,###.##");

    public AmmoXpTrackerPanel(AmmoXpTrackerPlugin plugin) {
        this.plugin = plugin;

        setBorder(new EmptyBorder(10, 10, 10, 10));
        setLayout(new BorderLayout());

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // Status section
        statusLabel = new JLabel("Not tracking");
        statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 16));
        statusLabel.setBorder(new EmptyBorder(0, 0, 10, 0));

        // Ammo type
        ammoTypeLabel = new JLabel("Ammo: None");
        ammoTypeLabel.setBorder(new EmptyBorder(5, 0, 5, 0));

        // Stats section
        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new GridLayout(3, 1, 0, 5));
        statsPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
        statsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        ammoUsedLabel = new JLabel("Ammo used: 0");
        totalXpGainedLabel = new JLabel("XP gained: 0");
        avgXpPerAmmoLabel = new JLabel("Avg XP per ammo: 0.00");

        statsPanel.add(ammoUsedLabel);
        statsPanel.add(totalXpGainedLabel);
        statsPanel.add(avgXpPerAmmoLabel);

        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 2, 10, 0));

        trackButton = new ColorJButton("Start Tracking", ColorScheme.BRAND_ORANGE);
        trackButton.setFocusPainted(false);
        trackButton.addActionListener(e -> {
            if (!plugin.isTracking()) {
                plugin.startTracking();
                trackButton.setText("Stop Tracking");
            } else {
                plugin.resetTracking();
                trackButton.setText("Start Tracking");
            }
            update();
        });

        resetButton = new ColorJButton("Reset", ColorScheme.PROGRESS_ERROR_COLOR);
        resetButton.setFocusPainted(false);
        resetButton.addActionListener(e -> {
            plugin.resetTracking();
            trackButton.setText("Start Tracking");
            update();
        });

        buttonPanel.add(trackButton);
        buttonPanel.add(resetButton);

        // Assemble panel
        centerPanel.add(statusLabel);
        centerPanel.add(ammoTypeLabel);
        centerPanel.add(statsPanel);

        add(centerPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void update() {
        SwingUtilities.invokeLater(() -> {
            if (plugin.isTracking()) {
                statusLabel.setText("Tracking Active");
                statusLabel.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
                trackButton.setText("Stop Tracking");
            } else {
                statusLabel.setText("Not tracking");
                statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
                trackButton.setText("Start Tracking");
            }

            ammoTypeLabel.setText("Ammo: " + plugin.getCurrentAmmoName());
            ammoUsedLabel.setText("Ammo used: " + formatter.format(plugin.getAmmoUsed()));
            totalXpGainedLabel.setText("XP gained: " + formatter.format(plugin.getTotalXpGained()));
            avgXpPerAmmoLabel.setText("Avg XP per ammo: " + formatter.format(plugin.getAvgXpPerAmmo()));
        });
    }
}


