package com.example.devices.service;

import com.example.devices.domain.Device;
import com.example.devices.domain.DeviceConstants;
import com.example.devices.domain.DeviceState;
import com.example.devices.exception.DeviceConflictException;
import java.util.Objects;

public final class DeviceStateValidator {
    private DeviceStateValidator() {
        // Utility class
    }

    public static void validateDelete(Device device) {
        if (device.getState() == DeviceState.IN_USE) {
            throw new DeviceConflictException(DeviceConstants.ERROR_DEVICE_IN_USE_DELETE);
        }
    }

    public static void validateUpdate(Device device, String newName, String newBrand) {
        if (device.getState() == DeviceState.IN_USE
                && (!Objects.equals(device.getName(), newName)
                    || !Objects.equals(device.getBrand(), newBrand))) {
            throw new DeviceConflictException(DeviceConstants.ERROR_DEVICE_IN_USE_UPDATE);
        }
    }
}
