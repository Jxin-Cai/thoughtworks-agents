CREATE TABLE IF NOT EXISTS cc_session (
    id VARCHAR(36) PRIMARY KEY,
    command VARCHAR(2000) NOT NULL,
    working_directory VARCHAR(500) NOT NULL,
    environment_variables CLOB,
    status VARCHAR(20) NOT NULL,
    exit_code INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    finished_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cc_session_status ON cc_session(status);

CREATE TABLE IF NOT EXISTS conversation (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    repository_full_name VARCHAR(200),
    cc_session_id VARCHAR(36),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_conversation_repository ON conversation(repository_full_name);
CREATE INDEX IF NOT EXISTS idx_conversation_status ON conversation(status);

CREATE TABLE IF NOT EXISTS message (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content CLOB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_message_conversation ON message(conversation_id);

CREATE TABLE IF NOT EXISTS dev_task (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    repository_full_name VARCHAR(200) NOT NULL,
    branch_name VARCHAR(200) NOT NULL,
    requirement CLOB NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dev_task_conversation ON dev_task(conversation_id);
CREATE INDEX IF NOT EXISTS idx_dev_task_repository ON dev_task(repository_full_name);
CREATE INDEX IF NOT EXISTS idx_dev_task_status ON dev_task(status);

CREATE TABLE IF NOT EXISTS task_phase (
    id VARCHAR(36) PRIMARY KEY,
    dev_task_id VARCHAR(36) NOT NULL,
    phase_type VARCHAR(20) NOT NULL,
    cc_session_id VARCHAR(36),
    output CLOB,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    failure_reason CLOB
);

CREATE INDEX IF NOT EXISTS idx_task_phase_dev_task ON task_phase(dev_task_id);

CREATE TABLE IF NOT EXISTS github_integration (
    id VARCHAR(36) PRIMARY KEY,
    access_token VARCHAR(500),
    token_type VARCHAR(50),
    scope VARCHAR(200),
    token_created_at TIMESTAMP,
    authenticated_user VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
