package com.minibloomberg.ui;

import java.awt.*;

public enum ToastType {
    INFO(new Color(30, 30, 30), Color.YELLOW),
    WARNING(new Color(50, 30, 0), Color.ORANGE),
    ERROR(new Color(30, 0, 0), Color.RED);

    public final Color background;
    public final Color foreground;

    ToastType(Color background, Color foreground) {
        this.background = background;
        this.foreground = foreground;
    }
}
