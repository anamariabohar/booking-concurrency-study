package org.anamaria.booking.system.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id")
    private User client;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String type;

    public enum Status {
        BOOKED, CANCELLED, NO_SHOW
    }
}