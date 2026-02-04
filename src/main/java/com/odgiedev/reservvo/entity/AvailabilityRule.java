package com.odgiedev.reservvo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "availability_rules")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AvailabilityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    // 0 = domingo, 6 = sábado
    @Column(nullable = false)
    private Integer dayOfWeek;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;
}