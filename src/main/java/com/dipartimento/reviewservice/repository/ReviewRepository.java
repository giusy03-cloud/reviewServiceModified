package com.dipartimento.reviewservice.repository;

import com.dipartimento.reviewservice.model.Review;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByEventId(Long eventId);
    List<Review> findByUserId(Long userId);

    @Query("SELECT r FROM Review r WHERE r.userId = :userId AND r.eventId = :eventId")
    List<Review> findByUserIdAndEventId(@Param("userId") Long userId,
                                        @Param("eventId") Long eventId);

    @Transactional
    void deleteByUserId(Long userId);

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    @Transactional
    void deleteByUserIdAndEventId(Long userId, Long eventId);

}