package com.example.devices.dto;

import com.example.devices.domain.DeviceConstants;
import com.example.devices.domain.DeviceState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeviceWriteRequest(
        @NotBlank @Size(max = DeviceConstants.MAX_FIELD_LENGTH) String name,
        @NotBlank @Size(max = DeviceConstants.MAX_FIELD_LENGTH) String brand,
        @NotNull DeviceState state) { }
