package com.example.devices.dto;

import com.example.devices.domain.DeviceConstants;
import com.example.devices.domain.DeviceState;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DevicePatchRequest(
        @Size(min = 1, max = DeviceConstants.MAX_FIELD_LENGTH) @Pattern(regexp = "(?s).*\\S.*") String name,
        @Size(min = 1, max = DeviceConstants.MAX_FIELD_LENGTH) @Pattern(regexp = "(?s).*\\S.*") String brand,
        DeviceState state) {

    @JsonIgnore
    @AssertTrue(message = "At least one of name, brand or state must be supplied")
    public boolean isNotEmpty() {
        return name != null || brand != null || state != null;
    }
}
