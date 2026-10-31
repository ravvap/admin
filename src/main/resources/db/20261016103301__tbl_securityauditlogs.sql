CREATE TABLE access_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    user_name VARCHAR(255),
    account_type VARCHAR(30) NOT NULL,
    login_date_time TIMESTAMP NOT NULL,
    logout_date_time TIMESTAMP,
    logout_status VARCHAR(30) NOT NULL,
    login_successful BOOLEAN NOT NULL,
    access_type VARCHAR(30) NOT NULL,
    create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL
);

-- Performance indexes for filtering and sorting (matching US-AL-1 through US-AL-6)
CREATE INDEX idx_login_time ON access_logs (login_date_time);
CREATE INDEX idx_user_id ON access_logs (user_id);
CREATE INDEX idx_account_type ON access_logs (account_type);
CREATE INDEX idx_login_successful ON access_logs (login_successful);


CREATE TABLE sa_splunk_event_log (
    id BIGSERIAL PRIMARY KEY,
    alert_name VARCHAR(100),
    event_time TIMESTAMP NOT NULL,
    operation VARCHAR(100),
    signature VARCHAR(100),
    request_type VARCHAR(100),
    user_id VARCHAR(150),
    actor_id VARCHAR(255),
    splunk_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Optional performance index on alert name and user ID for grid filtering
CREATE INDEX idx_splunk_event_log_alert_user ON sa_splunk_event_log (alert_name, user_id);

CREATE TABLE sa_security_alert (
    id BIGSERIAL PRIMARY KEY,
    severity VARCHAR(20) NOT NULL,
    condition_name VARCHAR(100) NOT NULL,
    trigger_type VARCHAR(30) NOT NULL,
    account_source VARCHAR(150),
    first_detected TIMESTAMP NOT NULL,
    last_seen TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL,
    correlation_id VARCHAR(100),
    resolution_note VARCHAR(500),
    handled_by VARCHAR(100)
);

-- Optional performance index for filtering alerts by status and severity on the grid UI
CREATE INDEX idx_security_alert_status_sev ON sa_security_alert (status, severity);