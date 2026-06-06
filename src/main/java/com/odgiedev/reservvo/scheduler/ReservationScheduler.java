package com.odgiedev.reservvo.scheduler;

import com.odgiedev.reservvo.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationScheduler {

    private final ReservationRepository reservationRepository;

    @Scheduled(cron = "0 0 */4 * * *")
    @Transactional
    public void completeExpiredReservations() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        int updated = reservationRepository.markExpiredAsCompleted(today, now);

        if (updated > 0) {
            log.info("Scheduler: {} reservas marcadas como COMPLETED", updated);
        } else {
            log.debug("Scheduler: nenhuma reserva expirada para completar");
        }
    }
}
