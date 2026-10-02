package com.example.devices.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record DevicePageResponse(List<DeviceResponse> content, int page, int size,
                                 long totalElements, int totalPages, boolean first, boolean last) {
    public static DevicePageResponse from(Page<DeviceResponse> page) {
        return new DevicePageResponse(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }
}
