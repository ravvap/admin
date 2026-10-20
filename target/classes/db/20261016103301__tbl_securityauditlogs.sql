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