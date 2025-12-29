package com.example.bookservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception levée quand le stock d'un livre est épuisé
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class OutOfStockException extends RuntimeException {

    public OutOfStockException(Long bookId) {
        super("Book with id " + bookId + " is out of stock");
    }

    public OutOfStockException(String message) {
        super(message);
    }
}
