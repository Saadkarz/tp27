package com.example.bookservice.service;

import com.example.bookservice.client.PricingClient;
import com.example.bookservice.dto.BookCreateDTO;
import com.example.bookservice.dto.BorrowResponseDTO;
import com.example.bookservice.entity.Book;
import com.example.bookservice.exception.BookNotFoundException;
import com.example.bookservice.exception.OutOfStockException;
import com.example.bookservice.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service métier pour la gestion des livres
 * Utilise un verrou pessimiste (FOR UPDATE) pour gérer la concurrence
 */
@Service
public class BookService {

    private static final Logger logger = LoggerFactory.getLogger(BookService.class);

    private final BookRepository bookRepository;
    private final PricingClient pricingClient;
    private final String instanceName;

    public BookService(
            BookRepository bookRepository,
            PricingClient pricingClient,
            @Value("${instance.name:unknown}") String instanceName) {
        this.bookRepository = bookRepository;
        this.pricingClient = pricingClient;
        this.instanceName = instanceName;
    }

    /**
     * Récupère tous les livres
     */
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    /**
     * Récupère un livre par son ID
     */
    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    /**
     * Crée un nouveau livre
     */
    public Book createBook(BookCreateDTO dto) {
        Book book = new Book(dto.getTitle(), dto.getAuthor(), dto.getStock());
        Book saved = bookRepository.save(book);
        logger.info("Created book: {}", saved);
        return saved;
    }

    /**
     * Emprunte un livre (décrémente le stock de 1)
     * Utilise un verrou pessimiste (FOR UPDATE) pour éviter les problèmes de
     * concurrence
     * 
     * @param bookId ID du livre à emprunter
     * @return DTO avec les détails de l'emprunt
     * @throws BookNotFoundException si le livre n'existe pas
     * @throws OutOfStockException   si le stock est épuisé
     */
    @Transactional
    public BorrowResponseDTO borrowBook(Long bookId) {
        logger.info("[{}] Attempting to borrow book id={}", instanceName, bookId);

        // Récupère le livre avec un verrou FOR UPDATE
        // Cela bloque les autres transactions qui tentent d'accéder à cette ligne
        Book book = bookRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        logger.info("[{}] Got lock on book: {} (stock={})", instanceName, book.getTitle(), book.getStock());

        // Vérifie le stock
        if (book.getStock() <= 0) {
            logger.warn("[{}] Book {} is out of stock!", instanceName, bookId);
            throw new OutOfStockException(bookId);
        }

        // Décrémente le stock
        book.setStock(book.getStock() - 1);
        bookRepository.save(book);

        logger.info("[{}] Borrowed book: {} (stock now={})", instanceName, book.getTitle(), book.getStock());

        // Récupère le prix depuis pricing-service (avec fallback)
        PricingClient.PriceResult priceResult = pricingClient.getPrice(bookId);

        // Prépare la réponse
        BorrowResponseDTO response = new BorrowResponseDTO();
        response.setBookId(bookId);
        response.setTitle(book.getTitle());
        response.setStockLeft(book.getStock());
        response.setPrice(priceResult.getPrice());
        response.setPricingFallback(priceResult.isFallback());
        response.setInstance(instanceName);
        response.setMessage("Book borrowed successfully");

        return response;
    }

    /**
     * Remet le stock d'un livre à une valeur donnée (pour les tests)
     */
    @Transactional
    public Book resetStock(Long bookId, int newStock) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));
        book.setStock(newStock);
        return bookRepository.save(book);
    }
}
