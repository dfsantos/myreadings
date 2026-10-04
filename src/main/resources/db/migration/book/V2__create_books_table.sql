CREATE TABLE books (
    id                TEXT PRIMARY KEY,
    user_id           TEXT NOT NULL REFERENCES users (id),
    title             TEXT NOT NULL,
    author            TEXT NOT NULL,
    publisher         TEXT,
    publication_year  INTEGER,
    page_count        INTEGER CHECK (page_count IS NULL OR page_count > 0),
    genre             TEXT,
    cover_url         TEXT,
    status            TEXT NOT NULL DEFAULT 'QUERO_LER',
    start_date        TEXT,
    end_date          TEXT,
    rating            INTEGER CHECK (rating IS NULL OR rating BETWEEN 1 AND 5),
    created_at        TEXT NOT NULL,
    updated_at        TEXT NOT NULL
);

CREATE INDEX idx_books_user_id ON books (user_id);
CREATE INDEX idx_books_user_status ON books (user_id, status);
CREATE INDEX idx_books_user_genre ON books (user_id, genre);
