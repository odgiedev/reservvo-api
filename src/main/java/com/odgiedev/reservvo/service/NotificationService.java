package com.odgiedev.reservvo.service;

import com.odgiedev.reservvo.entity.Notification;
import com.odgiedev.reservvo.entity.Reservation;
import com.odgiedev.reservvo.enums.NotificationType;
import com.odgiedev.reservvo.event.ReservationEvent;
import com.odgiedev.reservvo.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SesClient sesClient;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${aws.ses.from}")
    private String fromEmail;

    @Value("${aws.ses.from-name}")
    private String fromName;

    public void sendConfirmation(Reservation reservation) {
        eventPublisher.publishEvent(
                new ReservationEvent(this, reservation, NotificationType.CONFIRMATION));
    }

    public void sendCancellation(Reservation reservation) {
        eventPublisher.publishEvent(
                new ReservationEvent(this, reservation, NotificationType.CANCELLATION));
    }

    @Async
    @EventListener
    public void handleReservationEvent(ReservationEvent event) {
        Reservation reservation = event.getReservation();
        NotificationType type = event.getType();

        try {
            String subject = buildSubject(type);
            String body = buildBody(reservation, type);

            sendEmail(reservation.getClient().getEmail(), subject, body);
            saveNotification(reservation, type);

            log.info("Notificação enviada para {} | tipo: {} | reserva: {}",
                    reservation.getClient().getEmail(), type, reservation.getId());

        } catch (Exception e) {
            log.error("Erro ao enviar notificação | reserva: {} | erro: {}",
                    reservation.getId(), e.getMessage());
        }
    }

    private void sendEmail(String to, String subject, String htmlBody) {
        SendEmailRequest request = SendEmailRequest.builder()
                .source(fromName + " <" + fromEmail + ">")
                .destination(Destination.builder().toAddresses(to).build())
                .message(Message.builder()
                        .subject(Content.builder().data(subject).charset("UTF-8").build())
                        .body(Body.builder()
                                .html(Content.builder().data(htmlBody).charset("UTF-8").build())
                                .build())
                        .build())
                .build();

        sesClient.sendEmail(request);
    }

    private void saveNotification(Reservation reservation, NotificationType type) {
        Notification notification = Notification.builder()
                .reservation(reservation)
                .user(reservation.getClient())
                .type(type)
                .sentAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
    }

    private String buildSubject(NotificationType type) {
        return switch (type) {
            case CONFIRMATION -> "Reserva confirmada — Reservvo";
            case CANCELLATION -> "Reserva cancelada — Reservvo";
            default -> "Notificação — Reservvo";
        };
    }

    private String buildBody(Reservation reservation, NotificationType type) {
        String action = type == NotificationType.CONFIRMATION ? "confirmada" : "cancelada";
        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #333;">
                    <h2>Sua reserva foi %s!</h2>
                    <p><strong>Negócio:</strong> %s</p>
                    <p><strong>Recurso:</strong> %s</p>
                    <p><strong>Data:</strong> %s</p>
                    <p><strong>Horário:</strong> %s às %s</p>
                    %s
                    <br/>
                    <p>Atenciosamente,<br/><strong>Reservvo</strong></p>
                </body>
                </html>
                """.formatted(
                action,
                reservation.getResource().getProvider().getBusinessName(),
                reservation.getResource().getName(),
                reservation.getDate(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getNotes() != null
                        ? "<p><strong>Observações:</strong> " + reservation.getNotes() + "</p>"
                        : ""
        );
    }
}