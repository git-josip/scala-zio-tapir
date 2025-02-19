CREATE TABLE IF NOT EXISTS reviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    user_external_id VARCHAR(255),
    asin VARCHAR(255) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    review TEXT NOT NULL,
    helpful INT NOT NULL,
    images TEXT[],
    created TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated TIMESTAMPTZ NOT NULL DEFAULT now()
);