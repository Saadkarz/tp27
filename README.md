# TP27 - Test de Concurrence, Verrous DB & Résilience

**Auteur** : Karzouz Saad  
**Date** : 29/12/2024

---

## 📋 Objectifs du TP

Ce TP permet de vérifier :

1. ✅ **Emprunts concurrents** arrivent sur 3 instances (8081/8083/8084)
2. ✅ **Verrou DB** empêche le stock de devenir négatif
3. ✅ **Fallback** : Quand pricing-service tombe, book-service continue grâce au fallback
4. ✅ **Métriques Actuator** confirment que Retry et CircuitBreaker se déclenchent

---

## 🏗️ Architecture

```
                    ┌─────────────────────────────────────────────────────┐
                    │              Load Test (50 requêtes)                │
                    └─────────────────────────────────────────────────────┘
                                          │
                    ┌─────────────────────┼─────────────────────┐
                    ▼                     ▼                     ▼
            ┌───────────────┐     ┌───────────────┐     ┌───────────────┐
            │ book-service-1│     │ book-service-2│     │ book-service-3│
            │   (8081)      │     │   (8083)      │     │   (8084)      │
            └───────┬───────┘     └───────┬───────┘     └───────┬───────┘
                    │   Resilience4j      │                     │
                    │   (Retry+CB)        │                     │
                    └─────────────────────┼─────────────────────┘
                                          │
                    ┌─────────────────────┴─────────────────────┐
                    │                                           │
                    ▼                                           ▼
            ┌───────────────┐                           ┌───────────────┐
            │ pricing-svc   │                           │    MySQL      │
            │   (8082)      │                           │    (3307)     │
            └───────────────┘                           │   + Verrou   │
                                                        │   FOR UPDATE  │
                                                        └───────────────┘
```

---

## 🚀 Démarrage

```bash
# Lancer le stack Docker
docker compose up -d --build

# Vérifier que tout est UP
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
```

---

## 📊 Résultats des Tests avec Captures d'écran

### Partie A — Création du livre de test

Création d'un livre avec un stock de 10 exemplaires :

```bash
curl -X POST http://localhost:8081/api/books \
  -H "Content-Type: application/json" \
  -d '{"title":"TP-Concurrency","author":"Demo","stock":10}'
```

**Capture :**

![Partie A - Création du livre](Screenshots/Partie%20A.png)

---

### Partie C — Test de charge : 50 emprunts en parallèle

Lancement de 50 requêtes simultanées sur les 3 instances :

```powershell
.\loadtest.ps1 -BookId 1 -Requests 50
```

**Capture :**

![Partie C - Test de charge](Screenshots/Partie%20C.png)

---

### Partie E — Vérification du stock final

Après le test de charge, le stock est à 0 et **jamais négatif** :

**Capture :**

![Partie C&E - Stock final](Screenshots/Partie%20C%26E.png)

**Résultats :**
- ✅ Success (200) : 10 (= stock initial)
- ✅ Conflict (409) : 40 (= stock épuisé)
- ✅ Other : 0 (= aucune erreur)
- ✅ Stock final : **0** (jamais négatif !)

---

### Partie F — Test Fallback (pricing-service DOWN)

#### Étape F1 — Arrêt du pricing-service

```bash
docker compose stop pricing-service
```

**Capture :**

![Partie F1 - Stop pricing](Screenshots/Partie%20F1.png)

#### Étape F3 — Test avec fallback activé

```powershell
.\loadtest.ps1 -BookId 2 -Requests 30
```

**Capture :**

![Partie F3 - Fallback](Screenshots/Partie%20F3.png)

**Résultat :** Dans les réponses, on observe :
- `"price": 0.0` — Prix par défaut (fallback)
- `"pricingFallback": true` — Indicateur fallback activé

---

### Partie G — Métriques Resilience4j

Vérification des métriques disponibles dans Actuator :

```powershell
(Invoke-RestMethod http://localhost:8081/actuator/metrics).names | Select-String "resilience"
```

**Captures :**

![Partie G - Métriques Resilience4j](Screenshots/Partie%20G.png)

![Partie G - Détails métriques](Screenshots/Partie%20G%20part%202.png)

**Métriques disponibles :**
- `resilience4j.circuitbreaker.buffered.calls`
- `resilience4j.circuitbreaker.calls`
- `resilience4j.circuitbreaker.failure.rate`
- `resilience4j.circuitbreaker.not.permitted.calls`
- `resilience4j.circuitbreaker.slow.call.rate`
- `resilience4j.circuitbreaker.slow.calls`
- `resilience4j.circuitbreaker.state`
- `resilience4j.retry.calls`

---

## 🔐 Le Verrou DB (Pessimistic Locking)

### Pourquoi est-il nécessaire en multi-instances ?

Dans une architecture multi-instances, plusieurs requêtes peuvent arriver **simultanément** sur différentes instances et tenter de modifier la même ressource (le stock d'un livre).

**Sans verrou DB** :
```
Instance-1: lecture stock = 5
Instance-2: lecture stock = 5
Instance-1: stock = 5 - 1 = 4 → sauvegarde
Instance-2: stock = 5 - 1 = 4 → sauvegarde  ❌ Problème !
```
Résultat : 2 emprunts mais stock = 4 au lieu de 3.

**Avec verrou DB (FOR UPDATE)** :
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT b FROM Book b WHERE b.id = :id")
Optional<Book> findByIdForUpdate(@Param("id") Long id);
```

```
Instance-1: SELECT ... FOR UPDATE → verrouille la ligne
Instance-2: SELECT ... FOR UPDATE → ATTEND
Instance-1: stock = 5 - 1 = 4 → COMMIT → déverrouille
Instance-2: stock = 4 - 1 = 3 → COMMIT  ✅ Correct !
```

**Avantages** :
- Garantit l'intégrité des données
- Stock **jamais négatif**
- Fonctionne avec N instances

---

## 🔄 Circuit Breaker & Fallback

### Rôle du Circuit Breaker

Le **Circuit Breaker** protège le système en cas de défaillance d'un service externe (pricing-service).

```
                    CLOSED ───────────────────►  OPEN
                      │        (trop d'échecs)     │
                      │                            │
                      │    ◄─── HALF_OPEN ────►    │
                      │     (quelques appels       │
                      │          de test)          │
                      ▼                            ▼
              Appels normaux              Fallback immédiat
```

**États** :
- **CLOSED** : Fonctionnement normal, tous les appels passent
- **OPEN** : Trop d'échecs détectés, le circuit est ouvert, fallback immédiat
- **HALF_OPEN** : Test périodique pour voir si le service est rétabli

### Rôle du Fallback

Le **Fallback** fournit une valeur par défaut quand le service externe est indisponible :

```java
@CircuitBreaker(name = PRICING_SERVICE, fallbackMethod = "getPriceFallback")
@Retry(name = PRICING_SERVICE, fallbackMethod = "getPriceFallback")
public PriceResult getPrice(Long bookId) { ... }

// Fallback : prix = 0.0 quand pricing-service est DOWN
public PriceResult getPriceFallback(Long bookId, Exception ex) {
    return new PriceResult(0.0, true);  // prix = 0, fallback = true
}
```

**Avantages** :
- L'application reste **fonctionnelle** même si un service tombe
- L'utilisateur n'est pas bloqué
- Les métriques permettent de monitorer les problèmes

---

## 📁 Structure du Projet

```
tp27/
├── docker-compose.yml          # Orchestration des services
├── init.sql                    # Script d'initialisation MySQL
├── loadtest.ps1                # Script de test de charge (PowerShell)
├── loadtest.sh                 # Script de test de charge (Bash)
├── README.md                   # Ce fichier
│
├── Screenshots/                # Captures d'écran des tests
│   ├── Partie A.png           # Création du livre
│   ├── Partie C.png           # Test de charge
│   ├── Partie C&E.png         # Résultats et stock final
│   ├── Partie F1.png          # Arrêt pricing-service
│   ├── Partie F3.png          # Test fallback
│   ├── Partie G.png           # Métriques Resilience4j
│   └── Partie G part 2.png    # Détails métriques
│
├── pricing-service/            # Microservice de pricing
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/example/pricing/
│       ├── PricingServiceApplication.java
│       ├── controller/PricingController.java
│       └── service/PricingService.java
│
└── book-service/               # Microservice de gestion des livres
    ├── Dockerfile
    ├── pom.xml
    └── src/main/java/com/example/bookservice/
        ├── BookServiceApplication.java
        ├── client/PricingClient.java        # Client avec Resilience4j
        ├── config/RestTemplateConfig.java
        ├── controller/BookController.java
        ├── dto/
        │   ├── BookCreateDTO.java
        │   └── BorrowResponseDTO.java
        ├── entity/Book.java
        ├── exception/
        │   ├── BookNotFoundException.java
        │   ├── GlobalExceptionHandler.java
        │   └── OutOfStockException.java
        ├── repository/BookRepository.java   # Verrou FOR UPDATE
        └── service/BookService.java
```

---

## 🎯 Conclusion

Ce TP démontre avec succès :

### 1. Verrou DB pessimiste (FOR UPDATE)
- **Indispensable** en architecture multi-instances
- Garantit que le stock ne devient **jamais négatif**
- Sérialise les accès concurrents à une même ressource
- Chaque instance attend son tour pour modifier le stock

### 2. Resilience4j (Circuit Breaker + Retry + Fallback)
- **Retry** : Retente automatiquement en cas d'erreur transitoire
- **Circuit Breaker** : Coupe le circuit après plusieurs échecs pour éviter la surcharge du système
- **Fallback** : Fournit une réponse dégradée (`price = 0.0`) plutôt que de bloquer l'utilisateur

### 3. Observabilité
- Métriques Actuator pour monitorer Retry et CircuitBreaker
- Logs détaillés pour le debugging
- Indicateur `pricingFallback` dans les réponses pour savoir si le fallback a été utilisé

---

## 📚 Technologies utilisées

| Technologie | Version | Usage |
|-------------|---------|-------|
| Spring Boot | 3.2.1 | Framework principal |
| Resilience4j | 2.2.0 | Résilience (Retry, CB) |
| MySQL | 8.0 | Base de données |
| Docker | - | Conteneurisation |
| Micrometer | - | Métriques |
| Spring Actuator | - | Observabilité |

---

## 🔧 Commandes utiles

```bash
# Démarrer le stack
docker compose up -d --build

# Voir les logs
docker compose logs -f book-service-1

# Arrêter pricing-service (pour tester fallback)
docker compose stop pricing-service

# Relancer pricing-service
docker compose start pricing-service

# Test de charge
.\loadtest.ps1 -BookId 1 -Requests 50

# Vérifier les métriques
curl http://localhost:8081/actuator/metrics | jq '.names | map(select(. | contains("resilience")))'

# Arrêter tout
docker compose down
```
