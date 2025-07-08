package com.dipartimento.reviewservice.service;

import com.dipartimento.reviewservice.dto.UsersAccounts;
import com.dipartimento.reviewservice.model.Review;
import com.dipartimento.reviewservice.repository.ReviewRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class ReviewServ{

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private RestTemplate restTemplate;

    private final String AUTH_ME_URL = "http://localhost:8080/auth/me";       // User service endpoint per token validation
    private final String EVENT_SERVICE_URL = "http://localhost:8081/events";  // Event service endpoint
    private final String BOOKING_CHECK_URL = "http://localhost:8083/api/bookings/check"; // nuovo endpoint
    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    public List<Review> getReviewsByEvent(Long eventId) {
        return reviewRepository.findByEventId(eventId);
    }

    public Review saveReview(Review review) {
        return reviewRepository.save(review);
    }

    /**
     * Verifica che l'utente sia autenticato e che il token corrisponda allo userId passato
     */
    public boolean isUserAuthenticated(Long userId, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<UsersAccounts> response = restTemplate.exchange(
                    AUTH_ME_URL,
                    HttpMethod.GET,
                    entity,
                    UsersAccounts.class
            );

            UsersAccounts user = response.getBody();
            return user != null && user.getId() == userId;
            // puoi aggiungere controllo ruolo se vuoi
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica se l'evento esiste chiamando il microservizio Event
     */
    public boolean isEventExists(Long eventId) {
        try {
            String url = EVENT_SERVICE_URL + "/public/" + eventId;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean hasUserBookedEvent(Long userId, Long eventId, String token) {
        try {
            String url = BOOKING_CHECK_URL + "?userId=" + userId + "&eventId=" + eventId;

            System.out.println("📡 [DEBUG] Chiamata a BookingService: " + url);
            System.out.println("🔐 [DEBUG] Token usato: " + token);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Boolean> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Boolean.class
            );

            System.out.println("✅ [DEBUG] Risposta da BookingService: " + response.getStatusCode() + " - " + response.getBody());

            return Boolean.TRUE.equals(response.getBody());
        } catch (Exception e) {
            System.err.println("❌ [ERRORE] Eccezione nella chiamata a BookingService:");
            e.printStackTrace();
            return false;
        }
    }
    @Transactional
    public void deleteReviewsByUserId(Long userId) {
        reviewRepository.deleteByUserId(userId);
    }



}