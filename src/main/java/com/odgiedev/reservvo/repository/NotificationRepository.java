package com.odgiedev.reservvo.repository;

import com.odgiedev.reservvo.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
}