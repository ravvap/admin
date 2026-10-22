-- User Transaction Log: business-data change audit (who changed which value, when, and why)
CREATE TABLE user_transaction_log (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             VARCHAR(100)  NOT NULL,               -- who made the change (from change history / Javers)
    changed_at          TIMESTAMPTZ   NOT NULL,               -- when the change was saved (UI shows Eastern Time)
    cert_number         INTEGER       NOT NULL,               -- certificate # of the institution the record belongs to
    assessment_period   VARCHAR(7)    NOT NULL,               -- quarter the record belongs to, e.g. '2025 Q3' (not the change date)
    field_name          VARCHAR(100)  NOT NULL,               -- which field changed, e.g. 'CEO Address 1'
    transaction_type    VARCHAR(10)   NOT NULL,               -- CREATE or UPDATE, never DELETE
    reason_code         VARCHAR(20)   NOT NULL,               -- e.g. INSADRUPD, MRGCORR, SIMSTRAN
    reason_description  VARCHAR(255)  NOT NULL,               -- e.g. 'Updated general address'
    previous_value      VARCHAR(4000),
    current_value       VARCHAR(4000),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_utl_transaction_type   CHECK (transaction_type IN ('CREATE', 'UPDATE')),
    CONSTRAINT chk_utl_assessment_period  CHECK (assessment_period ~ '^[0-9]{4} Q[1-4]$')
);

CREATE INDEX idx_utl_changed_at        ON user_transaction_log (changed_at DESC);
CREATE INDEX idx_utl_period_changed_at ON user_transaction_log (assessment_period, changed_at DESC);
CREATE INDEX idx_utl_cert_number       ON user_transaction_log (cert_number);
CREATE INDEX idx_utl_user_id           ON user_transaction_log (user_id);
CREATE INDEX idx_utl_field_name        ON user_transaction_log (field_name);

-- Audit data is append-only
CREATE OR REPLACE FUNCTION fn_user_transaction_log_append_only() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'user_transaction_log is append-only (% not allowed)', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_user_transaction_log_append_only
    BEFORE UPDATE OR DELETE ON user_transaction_log
    FOR EACH ROW EXECUTE FUNCTION fn_user_transaction_log_append_only();
