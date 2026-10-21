package com.denkitronik.clienteservice.domain.repositories;

import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.entities.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("Integration tests (data slice) — IClienteDao con H2")
class IClienteDaoTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private IClienteDao clienteDao;

    private Region region;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        region = new Region();
        region.setNombre("Asia");
        em.persist(region);

        cliente = new Cliente();
        cliente.setNombre("Grace");
        cliente.setApellido("Hopper");
        cliente.setEmail("grace@navy.mil");
        cliente.setRegion(region);
        em.persist(cliente);

        em.flush();
    }

    @Test
    @DisplayName("findAllRegiones — devuelve las regiones persistidas")
    void findAllRegiones_debeRetornarListaDeRegiones() {
        List<Region> regiones = clienteDao.findAllRegiones();

        assertThat(regiones)
                .hasSize(1)
                .first()
                .extracting(Region::getNombre)
                .isEqualTo("Asia");
    }

    @Test
    @DisplayName("deleteById — elimina el cliente de la base de datos")
    void deleteById_debeEliminarElCliente() {
        Long id = cliente.getId();

        clienteDao.deleteById(id);
        em.flush();
        em.clear();

        assertThat(em.find(Cliente.class, id)).isNull();
    }

    @Test
    @DisplayName("findById — ID existente → devuelve el cliente")
    void findById_idExistente_debeRetornarCliente() {
        Optional<Cliente> resultado = clienteDao.findById(cliente.getId());

        assertThat(resultado)
                .isPresent()
                .get()
                .extracting(Cliente::getEmail)
                .isEqualTo("grace@navy.mil");
    }

    @Test
    @DisplayName("findById — ID inexistente → devuelve Optional vacío")
    void findById_idInexistente_debeRetornarVacio() {
        assertThat(clienteDao.findById(-1L)).isEmpty();
    }

    @Test
    @DisplayName("findAll paginado — devuelve la página solicitada")
    void findAll_paginado_debeRetornarPaginaDeClientes() {
        Page<Cliente> pagina = clienteDao.findAll(PageRequest.of(0, 1));

        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent())
                .singleElement()
                .extracting(Cliente::getNombre)
                .isEqualTo("Grace");
    }

    @Test
    @DisplayName("save — persiste un cliente nuevo")
    void save_clienteNuevo_debePersistirlo() {
        Cliente nuevo = new Cliente();
        nuevo.setNombre("Katherine");
        nuevo.setApellido("Johnson");
        nuevo.setEmail("katherine@nasa.gov");
        nuevo.setRegion(region);

        Cliente guardado = clienteDao.save(nuevo);
        em.flush();
        em.clear();

        assertThat(guardado.getId()).isNotNull();
        assertThat(clienteDao.findById(guardado.getId()))
                .isPresent()
                .get()
                .extracting(Cliente::getEmail)
                .isEqualTo("katherine@nasa.gov");
    }
}