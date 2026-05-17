-- Sequence for partitioned table (IDENTITY не поддерживается в партиционированных таблицах до PG 16)
CREATE SEQUENCE IF NOT EXISTS user_activities_id_seq;

-- Партиционированная таблица активностей (по диапазону дат)
CREATE TABLE IF NOT EXISTS user_activities (
    id             BIGINT      NOT NULL DEFAULT nextval('user_activities_id_seq'),
    user_id        BIGINT,
    event_type     VARCHAR(20),
    ip_address     VARCHAR(45),
    user_agent     TEXT,
    country        VARCHAR(100),
    city           VARCHAR(100),
    device_fingerprint VARCHAR(255),
    raw_payload    JSONB,
    created_at     TIMESTAMP   NOT NULL,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Партиции по годам
CREATE TABLE IF NOT EXISTS user_activities_2025
    PARTITION OF user_activities
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');

CREATE TABLE IF NOT EXISTS user_activities_2026
    PARTITION OF user_activities
    FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');

CREATE TABLE IF NOT EXISTS user_activities_2027
    PARTITION OF user_activities
    FOR VALUES FROM ('2027-01-01') TO ('2028-01-01');

-- Партиция по умолчанию для всего остального
CREATE TABLE IF NOT EXISTS user_activities_default
    PARTITION OF user_activities DEFAULT;

-- Индексы (создаются на родительской таблице, наследуются партициями)
CREATE INDEX IF NOT EXISTS idx_user_activities_user_id_event_type ON user_activities (user_id, event_type);
CREATE INDEX IF NOT EXISTS idx_user_activities_ip_address        ON user_activities (ip_address);
CREATE INDEX IF NOT EXISTS idx_user_activities_created_at        ON user_activities (created_at);

-- Индекс на JSONB для поиска по payload
CREATE INDEX IF NOT EXISTS idx_user_activities_raw_payload ON user_activities USING GIN (raw_payload);

-- Таблица алертов (без партиционирования — объём значительно меньше)
CREATE TABLE IF NOT EXISTS anomaly_alerts (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT,
    anomaly_type VARCHAR(50),
    severity     VARCHAR(20),
    description  VARCHAR(1024),
    ip_address   VARCHAR(45),
    detected_at  TIMESTAMP,
    resolved     BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_anomaly_alerts_user_id     ON anomaly_alerts (user_id);
CREATE INDEX IF NOT EXISTS idx_anomaly_alerts_detected_at ON anomaly_alerts (detected_at);
CREATE INDEX IF NOT EXISTS idx_anomaly_alerts_resolved    ON anomaly_alerts (resolved) WHERE resolved = FALSE;
