package org.anamaria.booking.system.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Provider {

    @Id
    private Long id; // same as user id

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private User user;

    private String specialization;

    private Integer avgAppointmentDuration;

    /** Used by optimistic locking booking strategy. */
    @Version
    private Long version;
}