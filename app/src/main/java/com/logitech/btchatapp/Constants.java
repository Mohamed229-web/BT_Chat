package com.logitech.btchatapp;

/**
 * Defines several constants used between {@link ChatController} and the UI.
 */
public interface Constants {

    // Message types sent from the ChatController Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final int REQUEST_ENABLE_BT = 3;

    // Key names received from the ChatController Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String DEVICE_OBJECT = "device_name";
    public static final String TOAST = "toast";

    public static int MESSAGE_WRITE_FILE = 6;
    public static int MESSAGE_RECEIVE_FILE = 7;
}
