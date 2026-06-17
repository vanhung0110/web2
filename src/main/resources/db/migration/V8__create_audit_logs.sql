-- V8__create_audit_logs.sql

CREATE SEQUENCE audit_log_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY DEFAULT nextval('audit_log_seq'),
    user_id UUID,
    username VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    request_uri VARCHAR(500),
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    tenant_id UUID,
    property_id UUID
);

CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id, timestamp DESC);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id, timestamp DESC);
CREATE INDEX idx_audit_logs_tenant ON audit_logs(tenant_id, timestamp DESC);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp DESC);
CREATE INDEX idx_audit_logs_action ON audit_logs(action, timestamp DESC);

-- Auto-cleanup audit logs older than 90 days (run via scheduled job)
COMMENT ON TABLE audit_logs IS 'Security audit log - retained 90 days, archived after';
