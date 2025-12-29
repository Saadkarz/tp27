package com.example.pricing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service de gestion des prix
 * Simule un catalogue de prix et permet de simuler une panne
 */
@Service
public class PricingService {

    private static final Logger logger = LoggerFactory.getLogger(PricingService.class);

    // État du service (UP = true, DOWN = false)
    private final AtomicBoolean serviceUp = new AtomicBoolean(true);

    // Catalogue de prix simulé (bookId -> price)
    private final Map<Long, Double> pricesCatalog = new HashMap<>();

    public PricingService() {
        // Initialisation du catalogue de prix par défaut
        pricesCatalog.put(1L, 19.99);
        pricesCatalog.put(2L, 24.99);
        pricesCatalog.put(3L, 14.99);
        pricesCatalog.put(4L, 29.99);
        pricesCatalog.put(5L, 9.99);
        logger.info("PricingService initialized with {} default prices", pricesCatalog.size());
    }

    /**
     * Récupère le prix d'un livre
     * 
     * @param bookId ID du livre
     * @return prix du livre
     * @throws RuntimeException si le service est en panne simulée
     */
    public double getPrice(Long bookId) {
        if (!serviceUp.get()) {
            logger.error("PricingService is DOWN - simulating failure for bookId={}", bookId);
            throw new RuntimeException("Pricing service is temporarily unavailable (simulated failure)");
        }

        // Retourne le prix du catalogue ou un prix par défaut basé sur l'ID
        Double price = pricesCatalog.getOrDefault(bookId, 10.0 + (bookId % 10) * 2.5);
        logger.info("PricingService: returning price {} for bookId={}", price, bookId);
        return price;
    }

    /**
     * Bascule l'état du service (UP <-> DOWN)
     * 
     * @return nouvel état (true = UP, false = DOWN)
     */
    public boolean toggleServiceState() {
        boolean newState = !serviceUp.get();
        serviceUp.set(newState);
        logger.warn("PricingService state toggled to: {}", newState ? "UP" : "DOWN");
        return newState;
    }

    /**
     * @return état actuel du service
     */
    public boolean isServiceUp() {
        return serviceUp.get();
    }
}
