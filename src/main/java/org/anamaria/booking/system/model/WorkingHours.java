package org.anamaria.booking.system.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "working_hours")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkingHours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    private Integer dayOfWeek; // 1 (Monday) to 7 (Sunday)

    private LocalTime startTime;

    private LocalTime endTime;
}