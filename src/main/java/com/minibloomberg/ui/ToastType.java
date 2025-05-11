package com.minibloomberg.ui;

import java.awt.Color;

public enum ToastType {
    INFO(ColorPalette.EERIE_BLACK, ColorPalette.ICTERINE),
    WARNING(ColorPalette.JET, ColorPalette.ORANGE_PEEL),
    ERROR(ColorPalette.JET, ColorPalette.RED);

    public final Color background;
    public final Color foreground;

    ToastType(Color background, Color foreground) {
        this.background = background;
        this.foreground = foreground;
    }
}
