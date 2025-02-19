CREATE TABLE IF NOT EXISTS products (
                                        id BIGSERIAL PRIMARY KEY,
                                        asin VARCHAR(255) UNIQUE NOT NULL,
                                        name TEXT UNIQUE NOT NULL,
                                        url TEXT NOT NULL,
                                        images TEXT[],
                                        created TIMESTAMPTZ NOT NULL DEFAULT now(),
                                        updated TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS users (
                                     id              BIGSERIAL
                                         PRIMARY KEY,
                                     email           TEXT
                                         UNIQUE
                                         NOT NULL,
                                     hashed_password TEXT
                                         NOT NULL
);

CREATE TABLE IF NOT EXISTS reviews (
                                       id BIGSERIAL PRIMARY KEY,
                                       user_id         BIGINT,
                                       FOREIGN KEY (user_id)
                                           REFERENCES users(id),
                                       user_external_id VARCHAR(255),
                                       asin VARCHAR(255) UNIQUE NOT NULL,
                                       title VARCHAR(255) NOT NULL,
                                       review TEXT NOT NULL,
                                       helpful INT NOT NULL,
                                       images TEXT[],
                                       created TIMESTAMPTZ NOT NULL DEFAULT now(),
                                       updated TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS recovery_tokens (
                                               email       TEXT
                                                   PRIMARY KEY,
                                               token       TEXT
                                                   NOT NULL,
                                               expiration  BIGINT
                                                   NOT NULL
);