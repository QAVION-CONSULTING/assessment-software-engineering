package com.qavion.timetracking.timeentry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    /** Optional filters: a null parameter means "do not filter". */
    @Query("""
            select e from TimeEntry e
            where (cast(:projectId as long) is null or e.projectId = :projectId)
              and (cast(:from as date) is null or e.entryDate >= :from)
              and (cast(:to as date) is null or e.entryDate <= :to)
            order by e.entryDate desc, e.id desc
            """)
    List<TimeEntry> search(@Param("projectId") Long projectId, @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /**
     * Takes a transaction-scoped PostgreSQL advisory lock for one calendar date and releases it on commit/rollback.
     * Concurrent writers for the same date queue up, so the "24 h per day" check-then-write cannot race.
     */
    @Query(value = "select 1 from (select pg_advisory_xact_lock(:dayKey)) as lock", nativeQuery = true)
    int lockDay(@Param("dayKey") long dayKey);

    /** Hours booked on a date across all projects (single-user assumption). */
    @Query("select coalesce(sum(e.durationHours), 0) from TimeEntry e where e.entryDate = :date")
    BigDecimal sumHoursByDate(@Param("date") LocalDate date);

    /** One grouped query for many projects, avoiding N+1 when listing. */
    @Query("""
            select new com.qavion.timetracking.timeentry.ProjectHours(e.projectId, sum(e.durationHours))
            from TimeEntry e where e.projectId in :projectIds group by e.projectId
            """)
    List<ProjectHours> sumHoursByProjects(@Param("projectIds") Collection<Long> projectIds);
}
