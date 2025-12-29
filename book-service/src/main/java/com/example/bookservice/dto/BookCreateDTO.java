package com.example.bookservice.dto;

/**
 * DTO pour créer un nouveau livre
 */
public class BookCreateDTO {
    private String title;
    private String author;
    private int stock;

    public BookCreateDTO() {
    }

    public BookCreateDTO(String title, String author, int stock) {
        this.title = title;
        this.author = author;
        this.stock = stock;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }
}
