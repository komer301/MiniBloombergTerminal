package com.minibloomberg.ui;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;
import java.util.Queue;

public class ToastManager {
    private static final Queue<JWindow> toastQueue = new LinkedList<>();
    private static boolean showing = false;

    public static void showToast(JFrame parent, String message, ToastType type) {
        JLabel label = new JLabel("<html><div style='text-align:center;'>" + message + "</div></html>", SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(type.background);
        label.setForeground(type.foreground);
        label.setFont(new Font("Consolas", Font.PLAIN, 14));
        label.setBorder(BorderFactory.createLineBorder(type.foreground));

        JWindow toast = new JWindow();
        toast.setBackground(new Color(0, 0, 0, 0));
        toast.getContentPane().add(label);
        toast.setSize(360, 60);

        Point location = parent.getLocationOnScreen();
        toast.setLocation(location.x + (parent.getWidth() - toast.getWidth()) / 2, location.y + 100);

        toastQueue.add(toast);
        if (!showing) {
            showNextToast();
        }
    }

    private static void showNextToast() {
        if (toastQueue.isEmpty()) {
            showing = false;
            return;
        }

        showing = true;
        JWindow toast = toastQueue.poll();
        toast.setOpacity(0f);
        toast.setVisible(true);

        Timer fadeIn = new Timer(30, null);
        fadeIn.addActionListener(e -> {
            float opacity = toast.getOpacity() + 0.1f;
            if (opacity >= 1f) {
                opacity = 1f;
                ((Timer) e.getSource()).stop();

                Timer pause = new Timer(2000, ev -> {
                    Timer fadeOut = new Timer(30, null);
                    fadeOut.addActionListener(ev2 -> {
                        float op = toast.getOpacity() - 0.1f;
                        if (op <= 0f) {
                            toast.dispose();
                            ((Timer) ev2.getSource()).stop();
                            showNextToast();
                        } else {
                            toast.setOpacity(op);
                        }
                    });
                    fadeOut.start();
                });
                pause.setRepeats(false);
                pause.start();
            }
            toast.setOpacity(opacity);
        });
        fadeIn.start();
    }
}
