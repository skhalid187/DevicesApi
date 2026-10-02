package com.example.devices.dto;

import com.example.devices.domain.Device;
import com.example.devices.domain.DeviceState;
import java.time.Instant;

public record DeviceResponse(
        Long id,
        String name,
        String brand,
        DeviceState state,
        Instant creationTime) {
    public static DeviceResponse from(Device device) {
        return new DeviceResponse(device.getId(), device.getName(), device.getBrand(),
                device.getState(), device.getCreationTime());
    }
}
