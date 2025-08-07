package com.dipartimento.reviewservice.controller;


import com.dipartimento.reviewservice.dto.ReviewDTO;
import com.dipartimento.reviewservice.model.Review;
import com.dipartimento.reviewservice.repository.ReviewRepository;
import com.dipartimento.reviewservice.service.ReviewServ;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewRepository repository;
    private final ReviewServ reviewService;

    @Autowired
    public ReviewController(ReviewRepository repository, ReviewServ reviewService) {
        this.repository = repository;
        this.reviewService = reviewService;
    }



    @PostMapping
    public ResponseEntity<?> createReview(
            @RequestBody ReviewDTO dto,
            @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token mancante o malformato");
        }

        String token = authHeader.substring(7);

        Long userIdFromToken = reviewService.extractUserIdFromToken(token);
        if (userIdFromToken == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Utente non autorizzato");
        }

        dto.setUserId(userIdFromToken);

        if (!reviewService.isUserAuthenticated(userIdFromToken, token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Utente non autorizzato");
        }

        if (dto.getEventId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("eventId mancante");
        }

        if (!reviewService.isEventExists(dto.getEventId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Evento non trovato");
        }

        if (!reviewService.hasUserBookedEvent(userIdFromToken, dto.getEventId(), token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Non hai prenotato questo evento");
        }

        if (!reviewService.isEventInPast(dto.getEventId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Puoi recensire solo a partire dal giorno successivo all'evento.");
        }

        // Se recensione già esistente, cancellala prima di salvare quella nuova
        if (repository.existsByUserIdAndEventId(userIdFromToken, dto.getEventId())) {
            reviewService.deleteReviewsByUserIdAndEventId(userIdFromToken, dto.getEventId());
        }

        Review review = new Review();
        review.setEventId(dto.getEventId());
        review.setUserId(userIdFromToken);
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());

        Review saved = repository.save(review);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }


    @GetMapping
    public List<Review> getAllReviews() {
        return repository.findAll();
    }




    @GetMapping("/event/{eventId}")
    public ResponseEntity<?> getReviewsByEvent(@PathVariable Long eventId,
                                               @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token mancante o malformato");
        }

        String token = authHeader.substring(7);
        Long userId = reviewService.extractUserIdFromToken(token);

        boolean isOrganizer = reviewService.isUserOrganizerOfEvent(userId, eventId, token);
        boolean hasBooked = reviewService.hasUserBookedEvent(userId, eventId, token);
        boolean hasRoleOrganizer = reviewService.hasRole(token, "ORGANIZER");

        System.out.println("[DEBUG] userId: " + userId + ", isOrganizer: " + isOrganizer + ", hasBooked: " + hasBooked + ", hasRoleOrganizer: " + hasRoleOrganizer);

        // Consenti accesso se è organizzatore dell'evento, oppure ha prenotato, oppure ha ruolo ORGANIZER
        if (!isOrganizer && !hasBooked && !hasRoleOrganizer) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accesso negato alle recensioni");
        }

        return ResponseEntity.ok(repository.findByEventId(eventId));
    }





    @DeleteMapping("/user/{userId}/event/{eventId}")
    public ResponseEntity<?> deleteReviewsByUserIdAndEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token mancante o malformato");
        }

        String token = authHeader.substring(7);
        Long loggedUserId = reviewService.extractUserIdFromToken(token);



        boolean isOrganizer = reviewService.isUserOrganizer(token);  // corretto
        // Organizer generico

        if (!userId.equals(loggedUserId) && !isOrganizer) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Non autorizzato");
        }


        reviewService.deleteReviewsByUserIdAndEventId(userId, eventId);  // dovrai implementare questo metodo nel service/repository
        return ResponseEntity.ok(Map.of("message", "Recensioni eliminate"));

    }




    @GetMapping("/me")
    public ResponseEntity<?> getMyReviews(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token mancante o malformato");
        }

        String token = authHeader.substring(7);
        Long userId = reviewService.extractUserIdFromToken(token);

        return ResponseEntity.ok(repository.findByUserId(userId));
    }

    @PutMapping("/user/{userId}/event/{eventId}")
    public ResponseEntity<?> updateReview(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestBody ReviewDTO dto,
            @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token mancante o malformato");
        }

        String token = authHeader.substring(7);
        Long loggedUserId = reviewService.extractUserIdFromToken(token);

        boolean isOrganizer = reviewService.isUserOrganizer(token);

        if (!userId.equals(loggedUserId) && !isOrganizer) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Non autorizzato");
        }

        if (!repository.existsByUserIdAndEventId(userId, eventId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Recensione non trovata");
        }

        List<Review> reviews = repository.findByUserIdAndEventId(userId, eventId);

        if (reviews.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Recensione non trovata");
        }

        Review review = reviews.get(0); // Prendi la prima recensione (se ce n’è più di una)

        review.setRating(dto.getRating());
        review.setComment(dto.getComment());

        Review updated = repository.save(review);

        return ResponseEntity.ok(updated);
    }









}