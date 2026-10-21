package com.denkitronik.clienteservice.delivery.exception;

import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.exception.ClienteNotFoundException;
import com.denkitronik.clienteservice.domain.exception.ClienteServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notFound_exposes404AndClientId() {
        var response = handler.manejarNotFound(new ClienteNotFoundException(42L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMensaje()).contains("42");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void serviceError_exposes500() {
        var response = handler.manejarServiceException(new ClienteServiceException("fallo"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMensaje()).contains("Error interno");
    }

    @Test
    void validationError_exposesFieldMessages() {
        var bindingResult = new BeanPropertyBindingResult(new Cliente(), "cliente");
        bindingResult.addError(new FieldError("cliente", "nombre", "no puede estar vacío"));
        var exception = new MethodArgumentNotValidException(null, bindingResult);

        var response = handler.manejarValidacion(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("nombre", "no puede estar vacío");
    }

    @Test
    void unexpectedError_exposesGeneric500() {
        var response = handler.manejarExcepcionGenerica(new IllegalStateException("detalle interno"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMensaje()).contains("Error inesperado");
    }
}
