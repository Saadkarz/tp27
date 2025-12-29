-- ===========================================
-- Script d'initialisation MySQL - TP27
-- Test de Concurrence & Verrous DB
-- ===========================================

-- Création de la base de données
CREATE DATABASE IF NOT EXISTS books 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

-- Utiliser la base de données
USE books;

-- Accorder tous les privilèges à l'utilisateur
GRANT ALL PRIVILEGES ON books.* TO 'booksuser'@'%';
FLUSH PRIVILEGES;

-- La table 'books' sera créée automatiquement par JPA (ddl-auto=update)
-- Structure attendue:
-- CREATE TABLE IF NOT EXISTS books (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     title VARCHAR(255) NOT NULL,
--     author VARCHAR(255) NOT NULL,
--     stock INT NOT NULL DEFAULT 0,
--     version BIGINT DEFAULT 0,
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT 'Database initialized successfully!' AS status;
