package com.example.bookservice.dto;

/**
 * DTO pour la réponse d'emprunt
 */
public class BorrowResponseDTO {
    private Long bookId;
    private String title;
    private int stockLeft;
    private double price;
    private boolean pricingFallback;
    private String instance;
    private String message;

    public BorrowResponseDTO() {
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getStockLeft() {
        return stockLeft;
    }

    public void setStockLeft(int stockLeft) {
        this.stockLeft = stockLeft;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public boolean isPricingFallback() {
        return pricingFallback;
    }

    public void setPricingFallback(boolean pricingFallback) {
        this.pricingFallback = pricingFallback;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
