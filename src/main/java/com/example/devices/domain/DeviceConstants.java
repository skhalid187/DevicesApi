package com.example.devices.domain;

public final class DeviceConstants {
    private DeviceConstants() {
        // Utility class
    }

    public static final int MAX_FIELD_LENGTH = 100;
    public static final String DEFAULT_SORT_FIELD = "id";

    // Error messages
    public static final String ERROR_BRAND_LENGTH = "brand must contain between 1 and 100 characters";
    public static final String ERROR_DEVICE_IN_USE_DELETE = "An in-use device cannot be deleted";
    public static final String ERROR_DEVICE_IN_USE_UPDATE = "Name and brand cannot be changed while the device is in use";
}
