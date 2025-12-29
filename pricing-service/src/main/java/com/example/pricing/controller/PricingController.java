package com.example.pricing.controller;

import com.example.pricing.service.PricingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller REST pour le service de pricing
 * Endpoints:
 * - GET /price/{bookId} : récupère le prix d'un livre
 * - POST /toggleDown : bascule l'état du service (UP/DOWN)
 * - GET /health-check : vérification de santé personnalisée
 */
@RestController
public class PricingController {

    private static final Logger logger = LoggerFactory.getLogger(PricingController.class);

    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    /**
     * Récupère le prix d'un livre
     * 
     * @param bookId ID du livre
     * @return JSON avec bookId et price
     */
    @GetMapping("/price/{bookId}")
    public ResponseEntity<Map<String, Object>> getPrice(@PathVariable Long bookId) {
        logger.info("Received price request for bookId={}", bookId);

        try {
            double price = pricingService.getPrice(bookId);

            Map<String, Object> response = new HashMap<>();
            response.put("bookId", bookId);
            response.put("price", price);
            response.put("currency", "EUR");
            response.put("status", "success");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            logger.error("Error getting price for bookId={}: {}", bookId, e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("bookId", bookId);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "error");

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        }
    }

    /**
     * Bascule l'état du service entre UP et DOWN
     * Utilisé pour simuler une panne et tester le fallback/circuit breaker
     * 
     * @return nouvel état du service
     */
    @PostMapping("/toggleDown")
    public ResponseEntity<Map<String, Object>> toggleServiceState() {
        boolean newState = pricingService.toggleServiceState();

        Map<String, Object> response = new HashMap<>();
        response.put("serviceUp", newState);
        response.put("status", newState ? "UP" : "DOWN");
        response.put("message", newState ? "Service is now UP - pricing requests will succeed"
                : "Service is now DOWN - pricing requests will fail (simulated failure)");

        logger.warn("Service state toggled via API: now {}", newState ? "UP" : "DOWN");

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint de vérification de santé personnalisé
     * 
     * @return état de santé du service
     */
    @GetMapping("/health-check")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("service", "pricing-service");
        health.put("status", pricingService.isServiceUp() ? "UP" : "DOWN (simulated)");
        health.put("simulatedFailure", !pricingService.isServiceUp());
        health.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(health);
    }

    /**
     * Endpoint racine pour vérification rapide
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, String>> root() {
        Map<String, String> response = new HashMap<>();
        response.put("service", "pricing-service");
        response.put("version", "1.0.0");
        response.put("status", "running");
        return ResponseEntity.ok(response);
    }
}
