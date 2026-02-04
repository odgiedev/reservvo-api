package com.odgiedev.reservvo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "resources")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private Integer slotDurationMin;

    @Column(nullable = false)
    private Boolean active = true;
}