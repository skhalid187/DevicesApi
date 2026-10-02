package com.example.devices.dto;

import com.example.devices.domain.DeviceState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeviceWriteRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String brand,
        @NotNull DeviceState state) { }
