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

        System.out.println("[DEBUG] createReview dto: " + dto);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token mancante o malformato");
        }

        String token = authHeader.substring(7);

        if (!reviewService.isUserAuthenticated(dto.getUserId(), token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Utente non autorizzato");
        }

        if (dto.getEventId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("eventId mancante");
        }


        if (!reviewService.isEventExists(dto.getEventId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Evento non trovato");
        }

        if (!reviewService.hasUserBookedEvent(dto.getUserId(), dto.getEventId(), token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Non hai prenotato questo evento");
        }

        if (!reviewService.isEventInPast(dto.getEventId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Puoi recensire solo a partire dal giorno successivo all'evento.");
        }

        if (repository.existsByUserIdAndEventId(dto.getUserId(), dto.getEventId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Hai già recensito questo evento.");
        }


        Review review = new Review();
        review.setEventId(dto.getEventId());
        review.setUserId(dto.getUserId());
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
    public List<Review> getReviewsByEvent(@PathVariable Long eventId) {
        return repository.findByEventId(eventId);
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<?> deleteReviewsByUserId(@PathVariable Long userId) {
        reviewService.deleteReviewsByUserId(userId);
        return ResponseEntity.ok("Recensioni eliminate");
    }

    @GetMapping("/can-review")
    public ResponseEntity<?> canUserReview(@RequestParam Long userId,
                                           @RequestParam Long eventId,
                                           @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token mancante");
        }

        String token = authHeader.substring(7);

        boolean isAuthenticated = reviewService.isUserAuthenticated(userId, token);
        boolean hasBooked = reviewService.hasUserBookedEvent(userId, eventId, token);
        boolean eventInPast = reviewService.isEventInPast(eventId);

        boolean canReview = isAuthenticated && hasBooked && eventInPast;

        return ResponseEntity.ok(canReview);
    }

    @GetMapping("/user/{userId}/event/{eventId}")
    public List<Review> getUserReviewsForEvent(@PathVariable Long userId, @PathVariable Long eventId) {
        return repository.findByUserIdAndEventId(userId, eventId);
    }

}