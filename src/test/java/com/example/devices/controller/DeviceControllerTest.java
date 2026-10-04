package com.example.devices.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.devices.domain.DeviceState;
import com.example.devices.dto.DevicePageResponse;
import com.example.devices.dto.DeviceResponse;
import com.example.devices.exception.DeviceConflictException;
import com.example.devices.exception.DeviceNotFoundException;
import com.example.devices.service.DeviceService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DeviceController.class)
class DeviceControllerTest {
    private static final String VALID = "{\"name\":\"Phone\",\"brand\":\"Acme\",\"state\":\"available\"}";
    private static final DeviceResponse DEVICE = new DeviceResponse(1L, "Phone", "Acme", DeviceState.AVAILABLE,
            Instant.parse("2026-01-01T00:00:00Z"));
    @Autowired MockMvc mvc;
    @MockitoBean DeviceService service;

    @Test
    void exposesCrudResponses() throws Exception {
        when(service.create(any())).thenReturn(DEVICE);
        when(service.findById(1)).thenReturn(DEVICE);
        when(service.replace(eq(1L), any())).thenReturn(DEVICE);
        when(service.patch(eq(1L), any())).thenReturn(DEVICE);
        mvc.perform(post("/api/devices").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/devices/1"))
                .andExpect(jsonPath("$.state").value("available"));
        mvc.perform(get("/api/devices/1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        mvc.perform(put("/api/devices/1").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/devices/1").contentType(MediaType.APPLICATION_JSON).content("{\"state\":\"inactive\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/devices/1")).andExpect(status().isNoContent()).andExpect(content().string(""));
    }

    @Test
    void bindsFiltersAndPagination() throws Exception {
        when(service.findAll("Acme", DeviceState.IN_USE, 1, 5))
                .thenReturn(new DevicePageResponse(List.of(), 1, 5, 0, 0, false, true));
        mvc.perform(get("/api/devices").param("brand", "Acme").param("state", "in-use")
                        .param("page", "1").param("size", "5"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void rejectsInvalidListFiltersBeforeCallingService() throws Exception {
        mvc.perform(get("/api/devices").param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/devices").param("size", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/devices").param("size", "101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/devices").param("brand", " ")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/devices").param("brand", "x".repeat(101))).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "null", "{", "{\"name\":\" \",\"brand\":\"Acme\",\"state\":\"available\"}",
            "{\"name\":\"Phone\",\"brand\":\"Acme\",\"state\":\"IN_USE\"}",
            "{\"name\":\"Phone\",\"brand\":\"Acme\",\"state\":null}"})
    void rejectsInvalidCreatesAndReplacements(String body) throws Exception {
        mvc.perform(post("/api/devices").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(put("/api/devices/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "null", "{\"name\":null}", "{\"brand\":null}", "{\"state\":null}",
            "{\"name\":\"  \"}", "{\"brand\":\"  \"}", "{\"state\":\"bad\"}",
            "{\"creationTime\":\"2026-01-01T00:00:00Z\"}", "{\"id\":2}", "{\"extra\":true}"})
    void rejectsInvalidPatches(String body) throws Exception {
        mvc.perform(patch("/api/devices/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsOversizeFields() throws Exception {
        mvc.perform(patch("/api/devices/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + "x".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void preservesHttpErrorStatusesAndHeaders() throws Exception {
        mvc.perform(post("/api/devices").contentType(MediaType.TEXT_PLAIN).content(VALID))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(post("/api/devices/1")).andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"));
        mvc.perform(get("/missing")).andExpect(status().isNotFound());
        mvc.perform(get("/api/devices/0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/devices/not-an-id")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/devices?state=IN_USE")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/devices?size=bad")).andExpect(status().isBadRequest());
    }

    @Test
    void mapsDomainErrors() throws Exception {
        when(service.findById(1)).thenThrow(new DeviceNotFoundException(1));
        mvc.perform(get("/api/devices/1")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.instance").value("/api/devices/1"));
        doThrow(new DeviceConflictException("In use")).when(service).delete(1);
        mvc.perform(delete("/api/devices/1")).andExpect(status().isConflict());
    }

    @Test
    void doesNotExposeInternalFailureDetails() throws Exception {
        when(service.findById(1)).thenThrow(new IllegalStateException("sensitive database detail"));
        mvc.perform(get("/api/devices/1")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }
}
