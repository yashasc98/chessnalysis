-- Create game_sessions table
CREATE TABLE IF NOT EXISTS game_sessions (
    id UUID PRIMARY KEY,
    white_player_id BIGINT NOT NULL,
    black_player_id BIGINT NOT NULL,
    time_control VARCHAR(32) NOT NULL,
    game_state VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP NOT NULL,
    last_activity_at TIMESTAMP NOT NULL,
    current_fen TEXT NOT NULL DEFAULT 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
    result VARCHAR(32),
    result_reason VARCHAR(256),
    CONSTRAINT fk_game_white_player FOREIGN KEY (white_player_id) REFERENCES users(id),
    CONSTRAINT fk_game_black_player FOREIGN KEY (black_player_id) REFERENCES users(id)
);

CREATE INDEX idx_game_white_player ON game_sessions(white_player_id);
CREATE INDEX idx_game_black_player ON game_sessions(black_player_id);
CREATE INDEX idx_game_state ON game_sessions(game_state);
CREATE INDEX idx_game_created_at ON game_sessions(created_at);

-- Create game_moves table
CREATE TABLE IF NOT EXISTS game_moves (
    id BIGSERIAL PRIMARY KEY,
    game_id UUID NOT NULL,
    move_number INT NOT NULL,
    from_square VARCHAR(4) NOT NULL,
    to_square VARCHAR(4) NOT NULL,
    move_uci VARCHAR(16) NOT NULL,
    san_notation VARCHAR(16),
    by_player_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_move_game FOREIGN KEY (game_id) REFERENCES game_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_move_player FOREIGN KEY (by_player_id) REFERENCES users(id)
);

CREATE INDEX idx_move_game_id ON game_moves(game_id);
CREATE INDEX idx_move_player_id ON game_moves(by_player_id);
CREATE INDEX idx_move_created_at ON game_moves(created_at);

-- Create match_queue_entries table
CREATE TABLE IF NOT EXISTS match_queue_entries (
    id BIGSERIAL PRIMARY KEY,
    queue_id UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    device_id VARCHAR(36) NOT NULL,
    time_control VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    matched_game_id UUID,
    metadata TEXT,
    CONSTRAINT fk_queue_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_queue_game FOREIGN KEY (matched_game_id) REFERENCES game_sessions(id)
);

CREATE INDEX idx_queue_user_id ON match_queue_entries(user_id);
CREATE INDEX idx_queue_status ON match_queue_entries(status);
CREATE INDEX idx_queue_time_control ON match_queue_entries(time_control);
CREATE INDEX idx_queue_created_at ON match_queue_entries(created_at);

