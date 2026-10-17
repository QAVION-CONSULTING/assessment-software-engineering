package com.example.timetracker.tracking;

import com.example.timetracker.project.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A block of working time on a single day, assigned to a project.
 * Duration is stored in minutes (450 = 7.5 h), at most 1440 (24 h).
 */
@Entity
@Table(name = "time_entries")
@Getter
@Setter(AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for JPA
public class TimeEntry {

    public static final int MAX_DURATION_MINUTES = 1440;

    public void replace(LocalDate newDate, Integer newDurationMinutes,
                        String newDescription, Project newProject) {
        this.date = newDate;
        this.durationMinutes = newDurationMinutes;
        this.description = newDescription;
        this.project = newProject;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The day the work was done; must not be in the future.
     */
    @NotNull
    @PastOrPresent
    @Column(nullable = false)
    private LocalDate date;

    /**
     * Duration in minutes (450 = 7.5 h); a single entry is at most 1440 (24 h).
     */
    @NotNull
    @Min(value = 1)
    @Max(value = 1440)
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Size(max = 500)
    @Column(length = 500)
    private String description; // optional

    /**
     * Optimistic-locking version, exposed via the If-Match header.
     */
    @Version
    @Column(nullable = false)
    private Long version;

    /**
     * The project this entry belongs to. Deleting a project removes
     * its entries (ON DELETE CASCADE at the DB level, mirrored by the
     * service layer before deletion).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_time_entries_project"))
    private Project project;

}