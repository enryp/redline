package com.metaformsystems.redline.repository;

import com.metaformsystems.redline.model.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    Optional<Participant> findByCorrelationId(String correlationId);

    Optional<Participant> findByParticipantContextId(String participantContextId);
}