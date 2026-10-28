package it.manuel.ristorante.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NonTrovatoException extends RuntimeException {

    public NonTrovatoException(String message) {
        super(message);
    }
}
