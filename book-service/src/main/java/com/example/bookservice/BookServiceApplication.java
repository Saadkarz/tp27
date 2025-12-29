package com.example.bookservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Book Service Application - TP27
 * Microservice de gestion des livres avec:
 * - Verrou DB pessimiste (FOR UPDATE) pour gérer la concurrence
 * - Resilience4j (Retry, CircuitBreaker) pour la résilience
 * - Fallback quand pricing-service est indisponible
 * 
 * @author Karzouz Saad
 */
@SpringBootApplication
public class BookServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookServiceApplication.class, args);
    }
}
