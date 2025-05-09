package com.minibloomberg.ui;

import javax.swing.*;
import java.awt.*;

public class FadeTransitionPanel extends JPanel {
    private Component currentComponent;
    private float alpha = 1f;
    private Timer fadeTimer;

    public FadeTransitionPanel() {
        setLayout(new CardLayout());
    }

    public void showWithFade(Component newComponent) {
        if (newComponent == null) return;

        // If there's no current, just add it directly
        if (currentComponent == null) {
            currentComponent = newComponent;
            add(currentComponent, "main");
            revalidate();
            repaint();
            return;
        }

        // Remove current and add new
        remove(currentComponent);
        add(newComponent, "main");
        currentComponent = newComponent;
        alpha = 0f;

        // Start fading in the new component
        fadeTimer = new Timer(15, e -> {
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
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            super.paintChildren(g2);
            g2.dispose();
        } else {
            super.paintChildren(g);
        }
    }
}
