package com.example.bookservice.controller;

import com.example.bookservice.dto.BookCreateDTO;
import com.example.bookservice.dto.BorrowResponseDTO;
import com.example.bookservice.entity.Book;
import com.example.bookservice.service.BookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller REST pour la gestion des livres
 * Endpoints:
 * - GET /api/books : liste tous les livres
 * - GET /api/books/{id} : récupère un livre par ID
 * - POST /api/books : crée un nouveau livre
 * - POST /api/books/{id}/borrow : emprunte un livre
 * - POST /api/books/{id}/reset-stock : remet le stock à une valeur donnée
 */
@RestController
@RequestMapping("/api/books")
public class BookController {

    private static final Logger logger = LoggerFactory.getLogger(BookController.class);

    private final BookService bookService;
    private final String instanceName;

    public BookController(
            BookService bookService,
            @Value("${instance.name:unknown}") String instanceName) {
        this.bookService = bookService;
        this.instanceName = instanceName;
    }

    /**
     * Liste tous les livres
     */
    @GetMapping
    public ResponseEntity<List<Book>> getAllBooks() {
        logger.info("[{}] GET /api/books", instanceName);
        return ResponseEntity.ok(bookService.getAllBooks());
    }

    /**
     * Récupère un livre par son ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        logger.info("[{}] GET /api/books/{}", instanceName, id);
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    /**
     * Crée un nouveau livre
     */
    @PostMapping
    public ResponseEntity<Book> createBook(@RequestBody BookCreateDTO dto) {
        logger.info("[{}] POST /api/books - Creating: {}", instanceName, dto.getTitle());
        Book created = bookService.createBook(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Emprunte un livre (décrémente le stock)
     */
    @PostMapping("/{id}/borrow")
    public ResponseEntity<BorrowResponseDTO> borrowBook(@PathVariable Long id) {
        logger.info("[{}] POST /api/books/{}/borrow", instanceName, id);
        BorrowResponseDTO response = bookService.borrowBook(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Remet le stock à une valeur donnée (pour les tests)
     */
    @PostMapping("/{id}/reset-stock")
    public ResponseEntity<Book> resetStock(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int stock) {
        logger.info("[{}] POST /api/books/{}/reset-stock?stock={}", instanceName, id, stock);
        Book updated = bookService.resetStock(id, stock);
        return ResponseEntity.ok(updated);
    }

    /**
     * Endpoint d'information sur l'instance
     */
    @GetMapping("/instance")
    public ResponseEntity<Map<String, String>> getInstanceInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("instance", instanceName);
        info.put("service", "book-service");
        info.put("status", "running");
        return ResponseEntity.ok(info);
    }
}
