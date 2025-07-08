package com.dipartimento.reviewservice.controller;

import com.dipartimento.reviewservice.dto.ReviewDTO;
import com.dipartimento.reviewservice.model.Review;
import com.dipartimento.reviewservice.repository.ReviewRepository;
import com.dipartimento.reviewservice.security.util.JwtUtil;
import com.dipartimento.reviewservice.service.ReviewServ;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewRepository repository;
    private final ReviewServ reviewServ;

    public ReviewController(ReviewRepository repository, ReviewServ reviewServ) {
        this.repository = repository;
        this.reviewServ = reviewServ;
    }

    @PostMapping
    public ResponseEntity<?> createReview(
            @RequestBody ReviewDTO dto,
            @RequestHeader("Authorization") String authHeader) {

        System.out.println("createReview: ricevuta richiesta per userId=" + dto.getUserId() + ", eventId=" + dto.getEventId());

        // Verifica header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("createReview: token mancante o malformato");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token mancante o malformato");
        }

        String token = authHeader.substring(7);
        System.out.println("createReview: token estratto");

        // 1. Verifica autenticazione
        if (!reviewServ.isUserAuthenticated(dto.getUserId(), token)) {
            System.out.println("createReview: utente non autenticato");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Utente non autorizzato");
        }
        System.out.println("createReview: utente autenticato");

        // 2. Verifica che l'evento esista
        if (!reviewServ.isEventExists(dto.getEventId())) {
            System.out.println("createReview: evento non trovato");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Evento non trovato");
        }
        System.out.println("createReview: evento trovato");

        // 3. Verifica che l'utente abbia prenotato l'evento
        if (!reviewServ.hasUserBookedEvent(dto.getUserId(), dto.getEventId(), token)) {
            System.out.println("createReview: utente non ha prenotato l'evento");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Non puoi recensire un evento che non hai prenotato");
        }
        System.out.println("createReview: utente ha prenotato l'evento");

        // 4. Crea e salva la recensione
        Review review = new Review();
        review.setEventId(dto.getEventId());
        review.setUserId(dto.getUserId());
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());

        Review saved = repository.save(review);
        System.out.println("createReview: recensione salvata con id=" + saved.getId());

        return ResponseEntity.ok(saved);
    }

    //metodo per richieste da html
    @PostMapping("/form")
    public ResponseEntity<Review> submitReview(@RequestParam Long eventId, @RequestParam Long userId, @RequestParam int rating, @RequestParam String comment) {
        System.out.println("submitReview: richiesta ricevita per userId=" + userId + ", eventId=" + eventId);

        Review review = new Review();
        review.setEventId(eventId);
        review.setUserId(userId);
        review.setRating(rating);
        review.setComment(comment);

        repository.save(review); // Salvo la recensione nel DB
        System.out.println("submitReview: recensione salvata");

        return ResponseEntity.ok(review); // Ritorno una risposta OK con l'oggetto Review
    }

    @GetMapping
    public List<Review> getAllReviews() {
        System.out.println("getAllReviews: richiesta per tutte le recensioni");
        return repository.findAll();
    }

    @GetMapping("/event/{eventId}")
    public List<Review> getReviewsByEvent(@PathVariable Long eventId) {
        System.out.println("getReviewsByEvent: richiesta recensioni per evento " + eventId);
        return repository.findByEventId(eventId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateReview(@PathVariable Long id,
                                          @RequestBody Review updatedReview,
                                          @RequestHeader("Authorization") String authHeader) {
        try {
            System.out.println("UpdateReview: richiesta di aggiornamento recensione con id = " + id);

            // Estraggo token JWT dall'header
            String token = authHeader.replace("Bearer ", "");
            System.out.println("Token ricevuto: " + token);

            Long userIdFromToken = JwtUtil.extractUserId(token);
            System.out.println("UserId estratto dal token: " + userIdFromToken);

            // Cerco la recensione esistente
            Review existingReview = repository.findById(id).orElse(null);
            if (existingReview == null) {
                System.out.println("Recensione con id " + id + " non trovata");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Recensione non trovata");
            }

            System.out.println("Recensione trovata con userId = " + existingReview.getUserId());

            // Controllo che chi fa la richiesta sia il proprietario della recensione
            if (!existingReview.getUserId().equals(userIdFromToken)) {
                System.out.println("Utente non autorizzato: userId token = " + userIdFromToken + ", userId recensione = " + existingReview.getUserId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Non sei autorizzato ad aggiornare questa recensione");
            }

            System.out.println("Aggiornamento commento da '" + existingReview.getComment() + "' a '" + updatedReview.getComment() + "'");
            System.out.println("Aggiornamento rating da '" + existingReview.getRating() + "' a '" + updatedReview.getRating() + "'");

            // Aggiorno i campi modificabili (esempio: comment e rating)
            existingReview.setComment(updatedReview.getComment());
            existingReview.setRating(updatedReview.getRating());

            // Salvo la recensione aggiornata
            repository.save(existingReview);
            System.out.println("Recensione aggiornata salvata con successo");

            return ResponseEntity.ok(existingReview);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore durante l'aggiornamento");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReview(@PathVariable Long id,
                                          @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long userIdFromToken = JwtUtil.extractUserId(token);
            String role = JwtUtil.extractUserRole(token);

            Review review = repository.findById(id).orElse(null);
            if (review == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Recensione non trovata");
            }

            // Controllo autorizzazione
            if (!review.getUserId().equals(userIdFromToken) && !"ORGANIZER".equals(role)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Non sei autorizzato a cancellare questa recensione");
            }

            repository.deleteById(id);
            return ResponseEntity.ok("Recensione cancellata con successo");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore interno del server");
        }
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<?> deleteReviewsByUserId(@PathVariable Long userId) {
        System.out.println("Eliminazione recensioni per userId: " + userId);
        repository.deleteByUserId(userId); // assicurati che esista questo metodo nel repository!
        return ResponseEntity.ok("Recensioni eliminate");
    }





}
