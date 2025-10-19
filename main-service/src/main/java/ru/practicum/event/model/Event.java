package ru.practicum.event.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.proxy.HibernateProxy;
import ru.practicum.category.Category;
import ru.practicum.event.dto.State;
import ru.practicum.request.Request;
import ru.practicum.user.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "initiator", nullable = false)
    User initiator;

    @ManyToOne
    @JoinColumn(name = "categories_id", nullable = false)
    Category category;

    @Column(length = 120, nullable = false)
    String title;

    @Column(length = 2000, nullable = false)
    String annotation;

    @Column(length = 7000, nullable = false)
    String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    State state;

    @Embedded
    Location location;

    @Column(name = "participant_limit", nullable = false)
    Long participantLimit;

    @Column(name = "request_moderation", nullable = false)
    Boolean requestModeration;

    @Column(nullable = false)
    Boolean paid;

    @Column(name = "event_date", nullable = false)
    LocalDateTime eventDate;

    @Column(name = "published_on")
    LocalDateTime publishedOn;

    @Column(name = "created_on", nullable = false)
    LocalDateTime createdOn;

    @OneToMany(mappedBy = "event", fetch = FetchType.LAZY)
    private List<Request> requests;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Event event = (Event) o;
        return getId() != null && Objects.equals(getId(), event.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" +
                "id = " + id + ", " +
                "initiator = " + initiator + ", " +
                "category = " + category + ", " +
                "title = " + title + ", " +
                "annotation = " + annotation + ", " +
                "description = " + description + ", " +
                "state = " + state + ", " +
                "location = " + location + ", " +
                "participantLimit = " + participantLimit + ", " +
                "requestModeration = " + requestModeration + ", " +
                "paid = " + paid + ", " +
                "eventDate = " + eventDate + ", " +
                "publishedOn = " + publishedOn + ", " +
                "createdOn = " + createdOn + ")";
    }
}
