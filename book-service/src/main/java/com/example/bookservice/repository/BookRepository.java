package com.example.bookservice.repository;

import com.example.bookservice.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

/**
 * Repository pour l'entité Book
 * Inclut une méthode avec verrou pessimiste (FOR UPDATE) pour gérer la
 * concurrence
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    /**
     * Récupère un livre avec un verrou pessimiste (SELECT ... FOR UPDATE)
     * Cela empêche les autres transactions de modifier la ligne jusqu'à ce que
     * la transaction courante soit terminée (commit ou rollback)
     * 
     * @param id ID du livre
     * @return le livre verrouillé
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Book b WHERE b.id = :id")
    Optional<Book> findByIdForUpdate(@Param("id") Long id);

    /**
     * Recherche un livre par titre
     */
    Optional<Book> findByTitle(String title);
}
