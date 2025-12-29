package com.example.bookservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client HTTP pour appeler le pricing-service
 * Utilise Resilience4j pour:
 * - Retry: réessayer en cas d'échec
 * - CircuitBreaker: couper le circuit après plusieurs échecs
 * - Fallback: retourner un prix par défaut (0.0) si le service est down
 */
@Service
public class PricingClient {

    private static final Logger logger = LoggerFactory.getLogger(PricingClient.class);
    private static final String PRICING_SERVICE = "pricingService";

    private final RestTemplate restTemplate;
    private final String pricingServiceUrl;

    public PricingClient(
            RestTemplate restTemplate,
            @Value("${pricing.service.url:http://localhost:8082}") String pricingServiceUrl) {
        this.restTemplate = restTemplate;
        this.pricingServiceUrl = pricingServiceUrl;
    }

    /**
     * Récupère le prix d'un livre depuis le pricing-service
     * Avec Retry et CircuitBreaker, et fallback vers 0.0 si échec
     * 
     * @param bookId ID du livre
     * @return le prix du livre, ou 0.0 en cas d'échec (fallback)
     */
    @CircuitBreaker(name = PRICING_SERVICE, fallbackMethod = "getPriceFallback")
    @Retry(name = PRICING_SERVICE, fallbackMethod = "getPriceFallback")
    public PriceResult getPrice(Long bookId) {
        String url = pricingServiceUrl + "/price/" + bookId;
        logger.info("Calling pricing-service: {}", url);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("price")) {
                double price = ((Number) response.get("price")).doubleValue();
                logger.info("Got price {} for bookId={}", price, bookId);
                return new PriceResult(price, false);
            }

            logger.warn("No price in response for bookId={}", bookId);
            throw new RuntimeException("Invalid response from pricing-service");

        } catch (Exception e) {
            logger.error("Error calling pricing-service for bookId={}: {}", bookId, e.getMessage());
            throw e; // Let Resilience4j handle it
        }
    }

    /**
     * Fallback method: retourne un prix de 0.0 quand pricing-service est
     * indisponible
     */
    public PriceResult getPriceFallback(Long bookId, Exception ex) {
        logger.warn("FALLBACK: pricing-service unavailable for bookId={}, using default price 0.0. Error: {}",
                bookId, ex.getMessage());
        return new PriceResult(0.0, true);
    }

    /**
     * Résultat contenant le prix et un indicateur de fallback
     */
    public static class PriceResult {
        private final double price;
        private final boolean fallback;

        public PriceResult(double price, boolean fallback) {
            this.price = price;
            this.fallback = fallback;
        }

        public double getPrice() {
            return price;
        }

        public boolean isFallback() {
            return fallback;
        }
    }
}
