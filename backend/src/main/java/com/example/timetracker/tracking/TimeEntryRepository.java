package com.example.timetracker.tracking;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    /**
     * Combined filters (AND), ordered by date ascending.
     * Null filters are simply not applied (handled by the service or via
     * nullable parameter checks below).
     */
    @Query("""
            select e from TimeEntry e
            where (:date is null or e.date = :date)
              and (:projectId is null or e.project.id = :projectId)
            order by e.date asc, e.id asc
            """)
    List<TimeEntry> findByFilters(@Param("date") LocalDate date,
                                  @Param("projectId") Long projectId);

    /**
     * Sum of all durations recorded for a date — used by the daily-cap rule.
     */
    @Query("select coalesce(sum(e.durationMinutes), 0) from TimeEntry e where e.date = :date")
    int totalMinutesForDate(@Param("date") LocalDate date);

    /**
     * Sum of all durations recorded for a date, excluding one entry
     * (during update, the entry's own old duration must not count).
     */
    @Query("""
            select coalesce(sum(e.durationMinutes), 0) from TimeEntry e
            where e.date = :date and e.id <> :excludeId
            """)
    int totalMinutesForDateExcluding(@Param("date") LocalDate date,
                                     @Param("excludeId") Long excludeId);

    /**
     * Total recorded minutes for a project — used by the budget status.
     */
    @Query("select coalesce(sum(e.durationMinutes), 0) from TimeEntry e where e.project.id = :projectId")
    long totalMinutesForProject(@Param("projectId") Long projectId);


    /**
     * Recorded minutes per project, for budget status and the monthly view.
     */
    @Query("""
            select e.project.id as projectId, coalesce(sum(e.durationMinutes), 0) as minutes
            from TimeEntry e
            group by e.project.id
            """)
    List<ProjectMinutes> totalMinutesPerProject();

    interface ProjectMinutes {
        Long getProjectId();

        long getMinutes();
    }

}