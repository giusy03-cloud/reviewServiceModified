package com.dipartimento.reviewservice.repository;

import com.dipartimento.reviewservice.model.Review;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByEventId(Long eventId);
    // List<Review> findByUserId(Long userId);

    @Transactional
    void deleteByUserId(Long userId);

}