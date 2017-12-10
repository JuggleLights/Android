package com.shiznatix.lightclubs.entities;

import java.util.Timer;

public class ScriptFrame {
    public long timerStart;
    public String message;
    public Timer timer;

    public ScriptFrame(long timerStart, String message) {
        this.timerStart = timerStart;
        this.message = message;
    }
}
