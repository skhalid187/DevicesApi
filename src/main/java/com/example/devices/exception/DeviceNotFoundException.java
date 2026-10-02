package com.example.devices.exception;

public class DeviceNotFoundException extends RuntimeException {
    public DeviceNotFoundException(long id) {
        super("Device " + id + " was not found");
    }
}
