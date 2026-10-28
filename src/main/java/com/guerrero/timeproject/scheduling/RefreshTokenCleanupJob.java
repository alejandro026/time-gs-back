package com.guerrero.timeproject.scheduling;

import com.guerrero.timeproject.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Limpia los refresh tokens vencidos una vez al día (con la rotación se crea uno por cada
 * renovación, así que la tabla crece sin parar).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "${app.jwt.refresh-cleanup-cron:0 17 3 * * *}", zone = "${TZ:America/Mexico_City}")
    @Transactional
    public void purgeExpired() {
        int removed = refreshTokenRepository.deleteExpiredBefore(Instant.now());
        if (removed > 0) log.info("Refresh tokens vencidos eliminados: {}", removed);
    }
}
