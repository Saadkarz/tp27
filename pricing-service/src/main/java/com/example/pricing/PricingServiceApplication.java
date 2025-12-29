package com.example.pricing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Pricing Service Application - TP27
 * Microservice responsable de la gestion des prix des livres
 * Dispose d'un endpoint pour simuler une panne (toggle ON/OFF)
 * 
 * @author Karzouz Saad
 */
@SpringBootApplication
public class PricingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PricingServiceApplication.class, args);
    }
}
