-- Permissions
CREATE TABLE permissions (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

-- Roles
CREATE TABLE roles (
    id   SERIAL PRIMARY KEY,
    key  VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- Role-Permission join table
CREATE TABLE role_permissions (
    role_id       INT NOT NULL REFERENCES roles(id),
    permission_id INT NOT NULL REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

-- Employee positions
CREATE TABLE employee_positions (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- Users (client_id FK added after clients table is created in V2)
CREATE TABLE users (
    id            UUID PRIMARY KEY,
    version       BIGINT       NOT NULL DEFAULT 0,
    role_id       INT          NOT NULL REFERENCES roles(id),
    first_name    VARCHAR(255) NOT NULL,
    last_name     VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    profile_url   VARCHAR(255),
    password_hash VARCHAR(255),
    position_id   INT REFERENCES employee_positions(id),
    client_id     UUID,
    titles        JSONB,
    status        VARCHAR(50)  NOT NULL CHECK (status IN ('PENDING', 'ACTIVE', 'DEACTIVATED')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email       ON users(email);
CREATE INDEX idx_users_role_id     ON users(role_id);
CREATE INDEX idx_users_position_id ON users(position_id);
CREATE INDEX idx_users_client_id   ON users(client_id);

-- User tokens
CREATE TABLE user_tokens (
    id         SERIAL PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES users(id),
    token      VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    type       VARCHAR(50)  NOT NULL CHECK (type IN ('ACCOUNT_ACTIVATION', 'PASSWORD_RESET', 'REFRESH_TOKEN')),
    UNIQUE (user_id, type)
);

CREATE INDEX idx_user_tokens_token   ON user_tokens(token);
CREATE INDEX idx_user_tokens_user_id ON user_tokens(user_id);
