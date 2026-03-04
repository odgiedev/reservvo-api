package com.odgiedev.reservvo.event;

import com.odgiedev.reservvo.entity.Reservation;
import com.odgiedev.reservvo.enums.NotificationType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ReservationEvent extends ApplicationEvent {

    private final Reservation reservation;
    private final NotificationType type;

    public ReservationEvent(Object source, Reservation reservation, NotificationType type) {
        super(source);
        this.reservation = reservation;
        this.type = type;
    }
}