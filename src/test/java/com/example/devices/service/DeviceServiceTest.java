package com.example.devices.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.devices.domain.Device;
import com.example.devices.domain.DeviceState;
import com.example.devices.dto.DevicePatchRequest;
import com.example.devices.dto.DeviceWriteRequest;
import com.example.devices.exception.DeviceConflictException;
import com.example.devices.exception.DeviceNotFoundException;
import com.example.devices.repository.DeviceRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {
    @Mock DeviceRepository repository;
    private DeviceService service;

    @BeforeEach
    void setUp() { service = new DeviceService(repository); }

    @Test
    void createsWithProvidedFields() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.create(new DeviceWriteRequest(" Phone ", " Acme ", DeviceState.AVAILABLE));
        assertThat(response.name()).isEqualTo(" Phone ");
        assertThat(response.brand()).isEqualTo(" Acme ");
    }

    @Test
    void fetchesDevice() {
        when(repository.findById(1L)).thenReturn(Optional.of(device(DeviceState.AVAILABLE)));
        assertThat(service.findById(1).name()).isEqualTo("Phone");
    }

    @Test
    void reportsMissingDevicesForEveryOperation() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        when(repository.findByIdForUpdate(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(9)).isInstanceOf(DeviceNotFoundException.class);
        assertThatThrownBy(() -> service.replace(9, new DeviceWriteRequest("P", "B", DeviceState.AVAILABLE)))
                .isInstanceOf(DeviceNotFoundException.class);
        assertThatThrownBy(() -> service.patch(9, new DevicePatchRequest(null, null, null)))
                .isInstanceOf(DeviceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(9)).isInstanceOf(DeviceNotFoundException.class);
    }

    @Test
    void replacesMutableFields() {
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(device(DeviceState.AVAILABLE)));
        var response = service.replace(1, new DeviceWriteRequest("Laptop", "Other", DeviceState.INACTIVE));
        assertThat(response.name()).isEqualTo("Laptop");
        assertThat(response.brand()).isEqualTo("Other");
        assertThat(response.state()).isEqualTo(DeviceState.INACTIVE);
    }

    @Test
    void patchPreservesOmittedValues() {
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(device(DeviceState.AVAILABLE)));
        DevicePatchRequest request = new DevicePatchRequest(" Tablet ", null, null);
        var response = service.patch(1, request);
        assertThat(response.name()).isEqualTo(" Tablet ");
        assertThat(response.brand()).isEqualTo("Acme");
        assertThat(response.state()).isEqualTo(DeviceState.AVAILABLE);
    }

    @Test
    void patchCanChangeBrandAndState() {
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(device(DeviceState.AVAILABLE)));
        DevicePatchRequest request = new DevicePatchRequest(null, " Other ", DeviceState.INACTIVE);
        var response = service.patch(1, request);
        assertThat(response.name()).isEqualTo("Phone");
        assertThat(response.brand()).isEqualTo(" Other ");
        assertThat(response.state()).isEqualTo(DeviceState.INACTIVE);
    }

    @Test
    void preventsInUseUpdatesAndDeletion() {
        Device device = device(DeviceState.IN_USE);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(device));
        DevicePatchRequest request = new DevicePatchRequest(null, "Other", null);
        assertThatThrownBy(() -> service.patch(1, request)).isInstanceOf(DeviceConflictException.class);
        assertThatThrownBy(() -> service.replace(1, new DeviceWriteRequest("New", "Acme", DeviceState.AVAILABLE)))
                .isInstanceOf(DeviceConflictException.class);
        assertThat(device.getName()).isEqualTo("Phone");
        assertThat(device.getBrand()).isEqualTo("Acme");
        assertThat(device.getState()).isEqualTo(DeviceState.IN_USE);
        assertThatThrownBy(() -> service.delete(1)).isInstanceOf(DeviceConflictException.class);
        verify(repository, never()).delete(any(Device.class));
    }

    @ParameterizedTest
    @EnumSource(DeviceState.class)
    void permitsStateOnlyChangesForInUseDevices(DeviceState targetState) {
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(device(DeviceState.IN_USE)));
        var response = service.replace(1, new DeviceWriteRequest("Phone", "Acme", targetState));
        assertThat(response.state()).isEqualTo(targetState);
    }

    @Test
    void deletesAvailableDevice() {
        Device device = device(DeviceState.AVAILABLE);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(device));
        service.delete(1);
        verify(repository).delete(device);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listsWithStablePagination() {
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(device(DeviceState.AVAILABLE)),
                        invocation.getArgument(1), 1));
        var page = service.findAll(" Acme ", DeviceState.AVAILABLE, 0, 20);
        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(page.first()).isTrue();
        assertThat(page.last()).isTrue();
        service.findAll(null, null, 0, 20);
    }

    private Device device(DeviceState state) { return new Device("Phone", "Acme", state); }
}
