-- Usuarios con roles (F6-API-01, RF-01 y RF-03). Contraseñas con BCrypt (coste 12), nunca en claro.
-- El ADMIN inicial lo crea la API en el primer arranque desde ADMIN_USERNAME / ADMIN_PASSWORD.

CREATE TABLE app_users (
    id            UUID          PRIMARY KEY,
    username      VARCHAR(40)   NOT NULL,
    full_name     VARCHAR(120)  NOT NULL,
    password_hash VARCHAR(100)  NOT NULL,
    role          VARCHAR(20)   NOT NULL,
    enabled       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX ux_app_users_username ON app_users (username);
