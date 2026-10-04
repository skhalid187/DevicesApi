package com.example.devices.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.devices.domain.Device;
import com.example.devices.domain.DeviceState;
import com.example.devices.repository.DeviceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DeviceApiIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired DeviceRepository repository;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void cleanDevices() { repository.deleteAll(); }

    @Test
    void migratesAnEmptyPostgresDatabaseAndEnforcesDatabaseConstraints() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class))
                .isEqualTo(1);
        long id = repository.saveAndFlush(new Device("Phone", "Acme", DeviceState.AVAILABLE)).getId();
        assertThatThrownBy(() -> jdbc.update("UPDATE devices SET creation_time = creation_time + interval '1 day' WHERE id = ?", id))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE devices SET state = 'BROKEN' WHERE id = ?", id))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE devices SET name = ' ' WHERE id = ?", id))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseGeneratesCreationTimeReturnedByApi() throws Exception {
        Instant before = databaseTime();
        JsonNode created = create("Phone", "Acme", DeviceState.AVAILABLE);
        Instant after = databaseTime();
        long id = created.get("id").asLong();
        Instant responseTime = Instant.parse(created.get("creationTime").asText());
        Instant storedTime = jdbc.queryForObject("SELECT creation_time FROM devices WHERE id = ?",
                (rs, rowNum) -> rs.getObject(1, OffsetDateTime.class).toInstant(), id);

        assertThat(responseTime).isBetween(before, after);
        assertThat(responseTime).isEqualTo(storedTime);
    }

    @ParameterizedTest
    @EnumSource(DeviceState.class)
    void createsAndFetchesEveryState(DeviceState state) throws Exception {
        JsonNode created = create(" Phone ", " Acme ", state);
        mvc.perform(get("/api/devices/{id}", created.get("id").asLong()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value(" Phone "))
                .andExpect(jsonPath("$.state").value(state.value()))
                .andExpect(jsonPath("$.creationTime").value(created.get("creationTime").asText()));
    }

    @Test
    void replacesPatchesAndDeletesWhilePreservingAuditTime() throws Exception {
        JsonNode created = create("Phone", "Acme", DeviceState.AVAILABLE);
        long id = created.get("id").asLong();
        String time = created.get("creationTime").asText();
        mvc.perform(put("/api/devices/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Laptop\",\"brand\":\"Other\",\"state\":\"inactive\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.creationTime").value(time));
        mvc.perform(patch("/api/devices/{id}", id).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Tablet\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.brand").value("Other"))
                .andExpect(jsonPath("$.state").value("inactive")).andExpect(jsonPath("$.creationTime").value(time));
        mvc.perform(delete("/api/devices/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/devices/{id}", id)).andExpect(status().isNotFound());
        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    void enforcesInUseRulesAtomicallyAndAllowsStateTransition() throws Exception {
        long id = create("Phone", "Acme", DeviceState.IN_USE).get("id").asLong();
        mvc.perform(patch("/api/devices/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New\",\"state\":\"available\"}"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/devices/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Phone\",\"brand\":\"Other\",\"state\":\"inactive\"}"))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/devices/{id}", id)).andExpect(status().isConflict());
        mvc.perform(get("/api/devices/{id}", id)).andExpect(jsonPath("$.name").value("Phone"))
                .andExpect(jsonPath("$.brand").value("Acme")).andExpect(jsonPath("$.state").value("in-use"));
        mvc.perform(put("/api/devices/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Phone\",\"brand\":\"Acme\",\"state\":\"in-use\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/devices/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"inactive\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/devices/{id}", id)).andExpect(status().isNoContent());
    }

    @Test
    void filtersByBrandStateAndBothWithStablePages() throws Exception {
        long first = create("One", "Acme", DeviceState.AVAILABLE).get("id").asLong();
        create("Two", "ACME", DeviceState.IN_USE);
        create("Three", "Other", DeviceState.AVAILABLE);
        mvc.perform(get("/api/devices?brand=acme")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/devices?state=available")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/devices?brand=acme&state=available")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].id").value(first));
        mvc.perform(get("/api/devices?page=0&size=1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3)).andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content[0].id").value(first));
        mvc.perform(get("/api/devices?page=99&size=1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
        mvc.perform(get("/api/devices?brand=missing")).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void missingMutationsReturnNotFound() throws Exception {
        mvc.perform(delete("/api/devices/999999")).andExpect(status().isNotFound());
        mvc.perform(patch("/api/devices/999999").contentType(MediaType.APPLICATION_JSON).content("{\"state\":\"inactive\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/devices/999999").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Phone\",\"brand\":\"Acme\",\"state\":\"available\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void servesDatabaseReadiness() throws Exception {
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    private JsonNode create(String name, String brand, DeviceState state) throws Exception {
        var body = mapper.createObjectNode().put("name", name).put("brand", brand).put("state", state.value());
        String response = mvc.perform(post("/api/devices").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response);
    }

    private Instant databaseTime() {
        return jdbc.queryForObject("SELECT clock_timestamp()",
                (rs, rowNum) -> rs.getObject(1, OffsetDateTime.class).toInstant());
    }
}
