-- KahootP46 Live - initial schema.
-- Rules and their sources are documented in docs/modelo-datos.md.

CREATE TABLE users (
    id            uuid         PRIMARY KEY,
    email         varchar(254) NOT NULL,
    password_hash varchar(100) NOT NULL,
    role          varchar(20)  NOT NULL,
    enabled       boolean      NOT NULL DEFAULT true,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_lowercase CHECK (email = lower(email)),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN'))
);

CREATE TABLE quizzes (
    id          uuid         PRIMARY KEY,
    owner_id    uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title       varchar(120) NOT NULL,
    description varchar(500),
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    version     bigint       NOT NULL DEFAULT 0,
    CONSTRAINT ck_quizzes_title_not_blank CHECK (length(trim(title)) > 0)
);
CREATE INDEX ix_quizzes_owner ON quizzes (owner_id, updated_at DESC);

CREATE TABLE questions (
    id          uuid         PRIMARY KEY,
    quiz_id     uuid         NOT NULL REFERENCES quizzes (id) ON DELETE CASCADE,
    text        varchar(300) NOT NULL,
    type        varchar(20)  NOT NULL,
    time_limit  integer      NOT NULL,
    order_index integer      NOT NULL,
    CONSTRAINT uk_questions_quiz_order UNIQUE (quiz_id, order_index),
    CONSTRAINT ck_questions_type CHECK (type IN ('SINGLE_CHOICE', 'TRUE_FALSE')),
    CONSTRAINT ck_questions_time_limit CHECK (time_limit BETWEEN 5 AND 240),
    CONSTRAINT ck_questions_order CHECK (order_index >= 0),
    CONSTRAINT ck_questions_text_not_blank CHECK (length(trim(text)) > 0)
);

CREATE TABLE choices (
    id          uuid         PRIMARY KEY,
    question_id uuid         NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    text        varchar(120) NOT NULL,
    is_correct  boolean      NOT NULL,
    order_index integer      NOT NULL,
    CONSTRAINT uk_choices_question_order UNIQUE (question_id, order_index),
    CONSTRAINT ck_choices_order CHECK (order_index BETWEEN 0 AND 5),
    CONSTRAINT ck_choices_text_not_blank CHECK (length(trim(text)) > 0)
);
-- At most one correct choice per question (the service also requires at least one).
CREATE UNIQUE INDEX uk_choices_single_correct ON choices (question_id) WHERE is_correct;

CREATE TABLE games (
    id                  uuid        PRIMARY KEY,
    quiz_id             uuid        NOT NULL REFERENCES quizzes (id) ON DELETE CASCADE,
    host_id             uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    pin                 varchar(6)  NOT NULL,
    status              varchar(20) NOT NULL,
    current_question    integer,
    total_questions     integer     NOT NULL,
    question_started_at timestamptz,
    question_ends_at    timestamptz,
    last_seq            bigint      NOT NULL DEFAULT 0,
    created_at          timestamptz NOT NULL DEFAULT now(),
    started_at          timestamptz,
    finished_at         timestamptz,
    version             bigint      NOT NULL DEFAULT 0,
    CONSTRAINT ck_games_pin CHECK (pin ~ '^[0-9]{6}$'),
    CONSTRAINT ck_games_status CHECK (status IN ('LOBBY', 'QUESTION_OPEN', 'ANSWER_REVEALED', 'FINISHED')),
    CONSTRAINT ck_games_total CHECK (total_questions >= 1),
    CONSTRAINT ck_games_current CHECK (current_question IS NULL OR current_question BETWEEN 1 AND total_questions)
);
-- A PIN identifies a single game while it is not finished.
CREATE UNIQUE INDEX uk_games_active_pin ON games (pin) WHERE status <> 'FINISHED';
CREATE INDEX ix_games_pin_created ON games (pin, created_at DESC);
CREATE INDEX ix_games_host_created ON games (host_id, created_at DESC);

-- Append-only log of every event broadcast to viewers (audit + ordering via seq).
CREATE TABLE game_events (
    id         bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    game_id    uuid        NOT NULL REFERENCES games (id) ON DELETE CASCADE,
    seq        bigint      NOT NULL,
    type       varchar(30) NOT NULL,
    payload    jsonb       NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_game_events_seq UNIQUE (game_id, seq),
    CONSTRAINT ck_game_events_type CHECK (type IN ('GAME_STARTED', 'QUESTION_STARTED', 'ANSWER_REVEALED',
                                                   'QUESTION_FINISHED', 'GAME_FINISHED'))
);
