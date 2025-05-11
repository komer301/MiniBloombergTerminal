package com.minibloomberg.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.Timer;

public class ToastManager {
    private static JWindow currentToast = null;
    private static Timer fadeInTimer, fadeOutTimer, pauseTimer;

    public static void showToast(JFrame parent, String message, ToastType type) {
        // Dispose of current toast if it's showing
        if (currentToast != null && currentToast.isVisible()) {
            stopCurrentTimers();
            currentToast.dispose();
        }

        JLabel label = new JLabel("<html><div style='text-align:center;'>" + message + "</div></html>", SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(type.background);
        label.setForeground(type.foreground);
        label.setFont(new Font("Consolas", Font.PLAIN, 14));
        label.setBorder(BorderFactory.createLineBorder(type.foreground));

        currentToast = new JWindow(parent);
        currentToast.setBackground(new Color(0, 0, 0, 0));
        currentToast.getContentPane().add(label);
        currentToast.setSize(360, 60);

        Point location = parent.getLocationOnScreen();
        currentToast.setLocation(location.x + (parent.getWidth() - currentToast.getWidth()) / 2, location.y + 100);
        currentToast.setOpacity(0f);
        currentToast.setVisible(true);

        fadeInTimer = new Timer(30, null);
        fadeInTimer.addActionListener(e -> {
            float opacity = currentToast.getOpacity() + 0.1f;
            if (opacity >= 1f) {
                opacity = 1f;
                currentToast.setOpacity(opacity);
                fadeInTimer.stop();

                pauseTimer = new Timer(2000, evt -> startFadeOut());
                pauseTimer.setRepeats(false);
                pauseTimer.start();
            } else {
                currentToast.setOpacity(opacity);
            }
        });
        fadeInTimer.start();
    }

    private static void startFadeOut() {
        fadeOutTimer = new Timer(30, null);
        fadeOutTimer.addActionListener(e -> {
            float opacity = currentToast.getOpacity() - 0.1f;
            if (opacity <= 0f) {
                currentToast.dispose();
                fadeOutTimer.stop();
            } else {
                currentToast.setOpacity(opacity);
            }
        });
        fadeOutTimer.start();
    }

    private static void stopCurrentTimers() {
        if (fadeInTimer != null) fadeInTimer.stop();
        if (fadeOutTimer != null) fadeOutTimer.stop();
        if (pauseTimer != null) pauseTimer.stop();
    }
}
