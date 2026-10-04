package com.example.devices.controller;

import com.example.devices.domain.DeviceConstants;
import com.example.devices.domain.DeviceState;
import com.example.devices.dto.DevicePageResponse;
import com.example.devices.dto.DevicePatchRequest;
import com.example.devices.dto.DeviceResponse;
import com.example.devices.dto.DeviceWriteRequest;
import com.example.devices.exception.InvalidDeviceRequestException;
import com.example.devices.service.DeviceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    private final DeviceService service;

    public DeviceController(DeviceService service) { this.service = service; }

    @GetMapping
    public DevicePageResponse findAll(@RequestParam(required = false) String brand,
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        String strippedBrand = brand != null ? brand.strip() : null;
        validateBrandFilter(strippedBrand);
        return service.findAll(strippedBrand, parseState(state), page, size);
    }

    private void validateBrandFilter(String brand) {
        if (brand != null) {
            if (brand.isEmpty() || brand.length() > DeviceConstants.MAX_FIELD_LENGTH) {
                throw new InvalidDeviceRequestException(DeviceConstants.ERROR_BRAND_LENGTH);
            }
        }
    }

    private DeviceState parseState(String state) {
        return state == null ? null : DeviceState.fromValue(state);
    }

    @GetMapping("/{id}")
    public DeviceResponse findById(@PathVariable @Positive long id) { return service.findById(id); }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DeviceResponse> create(@Valid @RequestBody DeviceWriteRequest request) {
        DeviceResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/devices/" + response.id())).body(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public DeviceResponse replace(@PathVariable @Positive long id, @Valid @RequestBody DeviceWriteRequest request) {
        return service.replace(id, request);
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public DeviceResponse patch(@PathVariable @Positive long id, @Valid @RequestBody DevicePatchRequest request) {
        return service.patch(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive long id) { service.delete(id); }
}
