package com.example.moco_project;

public class SwitchState {
    private static boolean isArActivity = false;

    public static boolean isArActivity() {
        return isArActivity;
    }

    public static void setArActivity(boolean isArActivity) {
        SwitchState.isArActivity = isArActivity;
    }
}
