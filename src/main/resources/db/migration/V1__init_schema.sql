CREATE TABLE city (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE theater (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    city_id BIGINT NOT NULL REFERENCES city(id),
    address VARCHAR(255)
);

CREATE TABLE screen (
    id BIGSERIAL PRIMARY KEY,
    theater_id BIGINT NOT NULL REFERENCES theater(id),
    name VARCHAR(50) NOT NULL,
    total_seats INT NOT NULL
);

CREATE TABLE seat (
    id BIGSERIAL PRIMARY KEY,
    screen_id BIGINT NOT NULL REFERENCES screen(id),
    seat_number VARCHAR(10) NOT NULL,
    seat_type VARCHAR(20) NOT NULL
);

CREATE TABLE movie (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    duration_minutes INT NOT NULL,
    language VARCHAR(50),
    genre VARCHAR(50)
);

-- Base price per seat category. One row per seat_type, admin-configurable.
CREATE TABLE seat_type_price (
    id BIGSERIAL PRIMARY KEY,
    seat_type VARCHAR(20) UNIQUE NOT NULL,
    base_price NUMERIC(10,2) NOT NULL
);

-- Single admin-configurable multiplier applied when a show's date is a Saturday/Sunday.
-- Deliberately a separate axis from seat_type_price.
CREATE TABLE weekend_pricing_rule (
    id BIGSERIAL PRIMARY KEY,
    multiplier NUMERIC(4,2) NOT NULL DEFAULT 1.00
);

CREATE TABLE show (
    id BIGSERIAL PRIMARY KEY,
    movie_id BIGINT NOT NULL REFERENCES movie(id),
    screen_id BIGINT NOT NULL REFERENCES screen(id),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL
);
-- No pricing FK here: price is computed at read/booking time from
-- seat_type_price x (weekend_pricing_rule if start_time is a weekend).

-- The seat map for a given show. One row per (show, seat), provisioned at show-creation time.
CREATE TABLE movie_show_seat (
    show_id BIGINT NOT NULL REFERENCES show(id),
    seat_id BIGINT NOT NULL REFERENCES seat(id),
    user_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    expires_at TIMESTAMP,
    PRIMARY KEY (show_id, seat_id)
);

CREATE TABLE discount_code (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    type VARCHAR(20) NOT NULL,
    value NUMERIC(10,2) NOT NULL,
    min_order_amount NUMERIC(10,2) DEFAULT 0,
    valid_from TIMESTAMP,
    valid_to TIMESTAMP
);

CREATE TABLE refund_policy (
    id BIGSERIAL PRIMARY KEY,
    cutoff_hours_before_show INT NOT NULL,
    refund_percentage INT NOT NULL
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL
);

CREATE TABLE booking (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    show_id BIGINT NOT NULL REFERENCES show(id),
    status VARCHAR(20) NOT NULL,
    total_amount NUMERIC(10,2) NOT NULL,
    discount_code_id BIGINT REFERENCES discount_code(id),
    idempotency_key VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (user_id, idempotency_key)
);

CREATE TABLE booking_seat (
    booking_id BIGINT NOT NULL REFERENCES booking(id),
    seat_id BIGINT NOT NULL REFERENCES seat(id),
    PRIMARY KEY (booking_id, seat_id)
);
