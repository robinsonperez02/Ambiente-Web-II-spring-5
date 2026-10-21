package com.denkitronik.clienteservice.integration;

import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.entities.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E tests — flujo completo HTTP → PostgreSQL con Testcontainers")
class ClienteIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String BASE = "/api/v1/cliente-service";
    private static final AtomicBoolean seeded = new AtomicBoolean(false);
    private static Long regionId;
    private static Long idCreado;

    @BeforeEach
    void seedDatosIniciales() {
        if (seeded.compareAndSet(false, true)) {
            jdbcTemplate.update("INSERT INTO regiones(nombre) VALUES (?)", "E2E Region");
            regionId = jdbcTemplate.queryForObject(
                    "SELECT id FROM regiones WHERE nombre = ? ORDER BY id DESC LIMIT 1",
                    Long.class,
                    "E2E Region");
        }
    }

    @Test
    @Order(1)
    @DisplayName("POST /clientes → 201 crea el cliente y devuelve su ID")
    void crearCliente_debeRetornar201() {
        Region region = new Region();
        region.setId(regionId);

        Cliente nuevo = new Cliente();
        nuevo.setNombre("Margaret");
        nuevo.setApellido("Hamilton");
        nuevo.setEmail("margaret@apollo.nasa");
        nuevo.setRegion(region);

        ResponseEntity<Cliente> respuesta =
                restTemplate.postForEntity(BASE + "/clientes", nuevo, Cliente.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getId()).isNotNull();
        assertThat(respuesta.getBody().getNombre()).isEqualTo("Margaret");

        idCreado = respuesta.getBody().getId();
    }

    @Test
    @Order(2)
    @DisplayName("GET /clientes/{id} → 200 encuentra el cliente creado")
    void buscarClienteCreado_debeRetornar200() {
        ResponseEntity<Cliente> respuesta =
                restTemplate.getForEntity(BASE + "/clientes/" + idCreado, Cliente.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getEmail()).isEqualTo("margaret@apollo.nasa");
    }

    @Test
    @Order(3)
    @DisplayName("GET /clientes → incluye el cliente creado")
    void listarClientes_debeIncluirClienteCreado() {
        ResponseEntity<Cliente[]> respuesta =
                restTemplate.getForEntity(BASE + "/clientes", Cliente[].class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(Arrays.stream(respuesta.getBody())
                .map(Cliente::getId))
                .contains(idCreado);
    }

    @Test
    @Order(4)
    @DisplayName("GET /clientes/page/0 → 200 devuelve la página")
    void listarClientesPaginado_debeRetornar200() {
        ResponseEntity<String> respuesta =
                restTemplate.getForEntity(BASE + "/clientes/page/0", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).contains("content");
    }

    @Test
    @Order(5)
    @DisplayName("PUT /clientes/{id} → 201 actualiza los datos")
    void actualizarCliente_debeRetornar201() {
        Region region = new Region();
        region.setId(regionId);

        Cliente actualizado = new Cliente();
        actualizado.setNombre("Margaret");
        actualizado.setApellido("Hamilton-Updated");
        actualizado.setEmail("margaret@apollo.nasa");
        actualizado.setRegion(region);

        HttpEntity<Cliente> entity = new HttpEntity<>(actualizado, headersJson());
        ResponseEntity<Cliente> respuesta = restTemplate.exchange(
                BASE + "/clientes/" + idCreado,
                HttpMethod.PUT,
                entity,
                Cliente.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getApellido()).isEqualTo("Hamilton-Updated");
    }

    @Test
    @Order(6)
    @DisplayName("GET /clientes/9999 → 404 para un ID inexistente")
    void idInexistente_debeRetornar404() {
        ResponseEntity<String> respuesta =
                restTemplate.getForEntity(BASE + "/clientes/9999", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(7)
    @DisplayName("DELETE /clientes/{id} → elimina y luego GET devuelve 404")
    void eliminarCliente_debeRetornar204YLuego404() {
        ResponseEntity<Void> borrado =
                restTemplate.exchange(BASE + "/clientes/" + idCreado,
                        HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
        ResponseEntity<String> despues =
                restTemplate.getForEntity(BASE + "/clientes/" + idCreado, String.class);

        assertThat(borrado.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(despues.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private HttpHeaders headersJson() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
