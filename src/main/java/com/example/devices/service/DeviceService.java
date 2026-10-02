package com.example.devices.service;

import com.example.devices.domain.Device;
import com.example.devices.domain.DeviceState;
import com.example.devices.dto.DevicePageResponse;
import com.example.devices.dto.DevicePatchRequest;
import com.example.devices.dto.DeviceResponse;
import com.example.devices.dto.DeviceWriteRequest;
import com.example.devices.exception.DeviceConflictException;
import com.example.devices.exception.DeviceNotFoundException;
import com.example.devices.exception.InvalidDeviceRequestException;
import com.example.devices.repository.DeviceRepository;
import java.util.Locale;
import java.util.Objects;
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
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidDeviceRequestException("page must be non-negative and size must be between 1 and 100");
        }
        Specification<Device> filter = (root, query, cb) -> cb.conjunction();
        if (brand != null) {
            String normalized = brand.strip().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty() || normalized.length() > 100) {
                throw new InvalidDeviceRequestException("brand must contain between 1 and 100 characters");
            }
            filter = filter.and((root, query, cb) -> cb.equal(cb.lower(root.get("brand")), normalized));
        }
        if (state != null) {
            filter = filter.and((root, query, cb) -> cb.equal(root.get("state"), state));
        }
        return DevicePageResponse.from(repository.findAll(filter,
                PageRequest.of(page, size, Sort.by("id"))).map(DeviceResponse::from));
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
        updateDevice(device, request.name() == null ? device.getName() : request.name(),
                request.brand() == null ? device.getBrand() : request.brand(),
                request.state() == null ? device.getState() : request.state());
        return DeviceResponse.from(device);
    }

    public void delete(long id) {
        Device device = findDeviceForUpdate(id);
        if (device.getState() == DeviceState.IN_USE) {
            throw new DeviceConflictException("An in-use device cannot be deleted");
        }
        repository.delete(device);
    }

    private void updateDevice(Device device, String name, String brand, DeviceState state) {
        if (device.getState() == DeviceState.IN_USE
                && (!Objects.equals(device.getName(), name) || !Objects.equals(device.getBrand(), brand))) {
            throw new DeviceConflictException("Name and brand cannot be changed while the device is in use");
        }
        device.update(name, brand, state);
    }

    private Device findDevice(long id) {
        return repository.findById(id).orElseThrow(() -> new DeviceNotFoundException(id));
    }

    private Device findDeviceForUpdate(long id) {
        return repository.findByIdForUpdate(id).orElseThrow(() -> new DeviceNotFoundException(id));
    }
}
