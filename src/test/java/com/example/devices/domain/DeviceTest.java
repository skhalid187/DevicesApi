package com.example.devices.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class DeviceTest {
    @ParameterizedTest
    @EnumSource(DeviceState.class)
    void stateWireValuesRoundTrip(DeviceState state) {
        assertThat(DeviceState.fromValue(state.value())).isEqualTo(state);
    }

    @ParameterizedTest
    @ValueSource(strings = {"IN_USE", "in_use", "unknown", "", " available "})
    void rejectsInvalidStates(String value) {
        assertThatThrownBy(() -> DeviceState.fromValue(value)).isInstanceOf(IllegalArgumentException.class);
    }
}
