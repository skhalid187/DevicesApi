package com.example.devices.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DeviceState {
    AVAILABLE("available"), IN_USE("in-use"), INACTIVE("inactive");

    private final String value;

    DeviceState(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static DeviceState fromValue(String value) {
        for (DeviceState state : values()) {
            if (state.value.equals(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("state must be available, in-use or inactive");
    }
}
