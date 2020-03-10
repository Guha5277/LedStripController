package com.guhafun;

public class WS2812B {
    static final int CONNECT = 1;
    static final int ON_OFF = 2;
    static final int PREV = 3;
    static final int NEXT = 4;
    static final int PAUSE = 5;
    static final int FAV = 6;
    static final int ACT_DEACT_MODE = 7;
    static final int SET_AUTO = 8;
    static final int SET_COLOR = 9;
    static final int SET_BRIGHT = 10;
    static final int SET_SPEED = 11;
    static final int SAVE_SETTINGS = 12;

    static final String[] modeNames = {"Rainbow Fade", "Rainbow Loop", "Random Burst", "Color Bounce", "Color Bounce Fade", "EMS Light One",
            "EMS Light ALL", "Flicker", "Pulse One Color", "Pulse with change color", "Fade Vertical", "Rule 30",
            "Random March", "RWB March", "Radiation", "Color Loop Verdelay", "White Temps", "Sin Bright Wave",
            "Pop Horizontal", "Quad Bright Cirve", "Flame", "Rainbow Vertical", "Pacman", "Random Color Pop",
            "EMS Lights Strobe", "RGB Propeller", "Kitt", "Matrix", "NEW! Rainbow Loop", "Color Wipe",
            "Cylon Bounce", "Fire", "Rainbow Cycle", "Twinkle Random", "Running Lights", "Sparkle",
            "Snow Sparkle", "Theater Chase", "Theater Chase Rainbow", "Strobe", "Bouncing Ball", "Bouncing Colored Ball",
            "Red", "Green", "Blue", "Yellow", "Cyan", "Purple", "White"};

    public static double[] speedMultiplier = {0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0};
}
