package com.odgiedev.reservvo.service;

import com.odgiedev.reservvo.config.RedisStreamsConfig;
import com.odgiedev.reservvo.entity.Notification;
import com.odgiedev.reservvo.entity.Reservation;
import com.odgiedev.reservvo.enums.NotificationType;
import com.odgiedev.reservvo.repository.NotificationRepository;
import com.odgiedev.reservvo.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@Component
@RequiredArgsConstructor
public class NotificationService implements StreamListener<String, MapRecord<String, String, String>> {

    private final SesClient sesClient;
    private final NotificationRepository notificationRepository;
    private final ReservationRepository reservationRepository;
    private final RedisTemplate<String, String> redisStreamTemplate;

    @Value("${aws.ses.from}")
    private String fromEmail;

    @Value("${aws.ses.from-name}")
    private String fromName;

    @Value("${aws.access-key-id:}")
    private String awsAccessKeyId;

    public void sendConfirmation(Reservation reservation) {
        publish(reservation, NotificationType.CONFIRMATION);
    }

    public void sendCancellation(Reservation reservation) {
        publish(reservation, NotificationType.CANCELLATION);
    }

    private void publish(Reservation reservation, NotificationType type) {
        Map<String, String> message = Map.of(
                "reservationId", reservation.getId().toString(),
                "type", type.name(),
                "attempt", "1"
        );

        redisStreamTemplate.opsForStream().add(
                MapRecord.create(RedisStreamsConfig.STREAM_KEY, message)
        );

        log.info("Evento publicado no stream | reserva: {} | tipo: {}", reservation.getId(), type);
    }

    @Async
    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        Map<String, String> body = message.getValue();
        RecordId recordId = message.getId();
        UUID reservationId = UUID.fromString(body.get("reservationId"));
        NotificationType type = NotificationType.valueOf(body.get("type"));
        int attempt = Integer.parseInt(body.getOrDefault("attempt", "1"));

        log.info("Processando notificação | reserva: {} | tipo: {} | tentativa: {}", reservationId, type, attempt);

        reservationRepository.findById(reservationId).ifPresentOrElse(
                reservation -> {
                    try {
                        String subject = buildSubject(type);
                        String html = buildBody(reservation, type);

                        sendEmail(reservation.getClient().getEmail(), subject, html);
                        saveNotification(reservation, type);

                        ack(recordId);

                        log.info("Email enviado e ACK confirmado | reserva: {} | tentativa: {}",
                                reservationId, attempt);

                    } catch (Exception e) {
                        log.error("Erro ao processar notificação | reserva: {} | tentativa: {} | erro: {}",
                                reservationId, attempt, e.getMessage());
                        handleRetry(body, recordId, reservationId, attempt);
                    }
                },
                () -> {
                    log.warn("Reserva não encontrada | id: {}", reservationId);

                    ack(recordId);
                }
        );
    }

    private void ack(RecordId recordId) {
        redisStreamTemplate.opsForStream().acknowledge(
                RedisStreamsConfig.STREAM_KEY,
                RedisStreamsConfig.CONSUMER_GROUP,
                recordId
        );
    }

    private void handleRetry(Map<String, String> originalBody, RecordId recordId,
                             UUID reservationId, int attempt) {
        if (attempt < RedisStreamsConfig.MAX_RETRY_ATTEMPTS) {
            int nextAttempt = attempt + 1;
            log.warn("Reagendando retry | reserva: {} | próxima tentativa: {}", reservationId, nextAttempt);

            Map<String, String> retryMessage = new HashMap<>(originalBody);
            retryMessage.put("attempt", String.valueOf(nextAttempt));

            redisStreamTemplate.opsForStream().add(
                    MapRecord.create(RedisStreamsConfig.STREAM_KEY, retryMessage)
            );

            ack(recordId);

        } else {
            log.error("Máximo de tentativas atingido | reserva: {} | enviando para DLQ", reservationId);
            sendToDlq(originalBody, reservationId);
            ack(recordId);
        }
    }

    private void sendToDlq(Map<String, String> originalBody, UUID reservationId) {
        Map<String, String> dlqMessage = new HashMap<>(originalBody);
        dlqMessage.put("failedAt", LocalDateTime.now().toString());
        dlqMessage.put("reason", "Max retry attempts reached");

        redisStreamTemplate.opsForStream().add(
                MapRecord.create(RedisStreamsConfig.DLQ_KEY, dlqMessage)
        );

        log.error("Mensagem enviada para DLQ | reserva: {} | stream: {}",
                reservationId, RedisStreamsConfig.DLQ_KEY);
    }

    private void sendEmail(String to, String subject, String htmlBody) {
        if (awsAccessKeyId.isBlank()) {
            log.warn("SES não configurado (sem credenciais) — email ignorado | destinatário: {}", to);
            return;
        }

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
        log.info("Email enviado via SES | destinatário: {} | assunto: {}", to, subject);
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