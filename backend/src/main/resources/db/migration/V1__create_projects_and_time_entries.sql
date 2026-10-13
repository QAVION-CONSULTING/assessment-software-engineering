CREATE TABLE projects (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name              VARCHAR(255)  NOT NULL,
    time_budget_hours NUMERIC(10, 2) NOT NULL,
    cost_budget_eur   NUMERIC(12, 2) NOT NULL,
    hourly_rate_eur   NUMERIC(10, 2) NOT NULL,
    CONSTRAINT ck_projects_name_not_blank CHECK (length(btrim(name)) > 0),
    CONSTRAINT ck_projects_time_budget_positive CHECK (time_budget_hours > 0),
    CONSTRAINT ck_projects_cost_budget_positive CHECK (cost_budget_eur > 0),
    CONSTRAINT ck_projects_hourly_rate_nonnegative CHECK (hourly_rate_eur >= 0)
);

CREATE TABLE time_entries (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id     BIGINT        NOT NULL,
    entry_date     DATE          NOT NULL,
    duration_hours NUMERIC(4, 2) NOT NULL,
    description    VARCHAR(1000),
    CONSTRAINT fk_time_entries_project FOREIGN KEY (project_id) REFERENCES projects (id),
    -- A single entry can never exceed one day; the 24 h/day total across entries is enforced in the service layer.
    CONSTRAINT ck_time_entries_duration_range CHECK (duration_hours > 0 AND duration_hours <= 24)
);

-- Per-project cost/budget aggregation and project-filtered lists.
CREATE INDEX ix_time_entries_project_id ON time_entries (project_id);
-- Daily total (24 h rule) and weekly/monthly reporting by date range.
CREATE INDEX ix_time_entries_entry_date ON time_entries (entry_date);
