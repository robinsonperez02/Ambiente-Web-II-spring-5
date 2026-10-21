package com.denkitronik.clienteservice.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ClienteChallengeH2Test {

    private static final String BASE = "/api/v1/cliente-service/clientes";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flujoCrearConsultarActualizarYEliminarClienteConH2() {
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM clientes", Long.class))
                .isZero();

        String suffix = UUID.randomUUID().toString();
        String regionName = "Challenge " + suffix;
        String email = "challenge-" + suffix + "@example.test";

        jdbcTemplate.update(
                "INSERT INTO regiones(nombre) VALUES (?) ON CONFLICT DO NOTHING",
                regionName);
        Long regionId = jdbcTemplate.queryForObject(
                "SELECT id FROM regiones WHERE nombre = ? ORDER BY id DESC LIMIT 1",
                Long.class,
                regionName);

        Map<String, Object> cliente = Map.of(
                "id", 999L,
                "nombre", "Challenge",
                "apellido", "Original",
                "email", email,
                "region", Map.of("id", regionId));

        ResponseEntity<Map> creado = restTemplate.postForEntity(BASE, cliente, Map.class);
        assertThat(creado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(creado.getBody()).isNotNull();
        Object id = creado.getBody().get("id");
        assertThat(id).isNotNull();
        assertThat(((Number) id).longValue()).isNotEqualTo(999L);

        ResponseEntity<Map> consultado = restTemplate.getForEntity(BASE + "/" + id, Map.class);
        assertThat(consultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(consultado.getBody()).containsEntry("email", email);

        Map<String, Object> actualizado = Map.of(
                "nombre", "Challenge",
                "apellido", "Actualizado",
                "email", email,
                "region", Map.of("id", regionId));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> respuestaUpdate = restTemplate.exchange(
                BASE + "/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(actualizado, headers),
                Map.class);
        assertThat(respuestaUpdate.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuestaUpdate.getBody()).containsEntry("apellido", "Actualizado");

        ResponseEntity<Void> eliminado = restTemplate.exchange(
                BASE + "/" + id, HttpMethod.DELETE, null, Void.class);
        assertThat(eliminado.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(restTemplate.getForEntity(BASE + "/" + id, Map.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
