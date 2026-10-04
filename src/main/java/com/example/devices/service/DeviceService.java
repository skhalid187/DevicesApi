package com.example.devices.service;

import com.example.devices.domain.Device;
import com.example.devices.domain.DeviceConstants;
import com.example.devices.domain.DeviceState;
import com.example.devices.dto.DevicePageResponse;
import com.example.devices.dto.DevicePatchRequest;
import com.example.devices.dto.DeviceResponse;
import com.example.devices.dto.DeviceWriteRequest;
import com.example.devices.exception.DeviceNotFoundException;
import com.example.devices.repository.DeviceRepository;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeviceService {
    private final DeviceRepository repository;

    public DeviceService(DeviceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public DevicePageResponse findAll(String brand, DeviceState state, int page, int size) {
        Specification<Device> filter = (root, query, cb) -> cb.conjunction();
        if (brand != null) {
            String normalized = brand.toLowerCase(Locale.ROOT);
            filter = filter.and((root, query, cb) -> cb.equal(cb.lower(root.get("brand")), normalized));
        }
        if (state != null) {
            filter = filter.and((root, query, cb) -> cb.equal(root.get("state"), state));
        }
        return DevicePageResponse.from(repository.findAll(filter,
                PageRequest.of(page, size, Sort.by(DeviceConstants.DEFAULT_SORT_FIELD))).map(DeviceResponse::from));
    }

    @Transactional(readOnly = true)
    public DeviceResponse findById(long id) {
        return DeviceResponse.from(findDevice(id));
    }

    public DeviceResponse create(DeviceWriteRequest request) {
        Device device = new Device(request.name(), request.brand(), request.state());
        return DeviceResponse.from(repository.save(device));
    }

    public DeviceResponse replace(long id, DeviceWriteRequest request) {
        Device device = findDeviceForUpdate(id);
        updateDevice(device, request.name(), request.brand(), request.state());
        return DeviceResponse.from(device);
    }

    public DeviceResponse patch(long id, DevicePatchRequest request) {
        Device device = findDeviceForUpdate(id);
        updateDevice(device,
                request.name() != null ? request.name() : device.getName(),
                request.brand() != null ? request.brand() : device.getBrand(),
                request.state() != null ? request.state() : device.getState());
        return DeviceResponse.from(device);
    }

    public void delete(long id) {
        Device device = findDeviceForUpdate(id);
        DeviceStateValidator.validateDelete(device);
        repository.delete(device);
    }

    private void updateDevice(Device device, String name, String brand, DeviceState state) {
        DeviceStateValidator.validateUpdate(device, name, brand);
        device.update(name, brand, state);
    }

    private Device findDevice(long id) {
        return repository.findById(id).orElseThrow(() -> new DeviceNotFoundException(id));
    }

    private Device findDeviceForUpdate(long id) {
        return repository.findByIdForUpdate(id).orElseThrow(() -> new DeviceNotFoundException(id));
    }
}
