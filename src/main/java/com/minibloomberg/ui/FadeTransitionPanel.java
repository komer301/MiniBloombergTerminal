package com.minibloomberg.ui;

import java.awt.AlphaComposite;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;
import javax.swing.Timer;

public class FadeTransitionPanel extends JPanel {
    private Component currentComponent;
    private float alpha = 1f; // Opacity for fade effect
    private Timer fadeTimer;

    public FadeTransitionPanel() {
        setLayout(new CardLayout());
        setBackground(ColorPalette.RED); // Temporary background for visibility
    }

    public void showWithFade(Component newComponent) {
        if (newComponent == null) return;

        // First time display, no need to fade
        if (currentComponent == null) {
            currentComponent = newComponent;
            add(currentComponent, "main");
            revalidate();
            repaint();
            return;
        }

        // Switch to new component and start fade effect
        remove(currentComponent);
        add(newComponent, "main");
        currentComponent = newComponent;
        alpha = 0f;

        // Timer gradually increases opacity
        fadeTimer = new Timer(10, e -> {
            alpha += 0.05f;
            if (alpha >= 1f) {
                alpha = 1f;
                fadeTimer.stop();
            }
            repaint();
        });

        fadeTimer.start();
    }

    @Override
    protected void paintChildren(Graphics g) {
        if (alpha < 1f && currentComponent != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)); // Apply opacity
            super.paintChildren(g2);
            g2.dispose();
        } else {
            super.paintChildren(g);
        }
    }
}
