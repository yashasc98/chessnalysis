package com.chessnalysis.service.game;

import com.chessnalysis.domain.game.Game;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * In-memory game registry for active games.
 * Uses read-write locks to ensure single-writer concurrency per game.
 * Ensures only one thread modifies a game at a time.
 */
@Slf4j
@Service
public class GameRegistry {

    private final Map<UUID, Game> games = new ConcurrentHashMap<>();
    private final Map<UUID, ReadWriteLock> locks = new ConcurrentHashMap<>();

    /**
     * Register a new game in the registry.
     */
    public void register(Game game) {
        locks.putIfAbsent(game.getId(), new ReentrantReadWriteLock());
        games.put(game.getId(), game);
        log.info("Game registered: {}", game.getId());
    }

    /**
     * Get a game from the registry (read-only).
     */
    public Optional<Game> get(UUID gameId) {
        return Optional.ofNullable(games.get(gameId));
    }

    /**
     * Execute a write operation on a game with single-writer lock.
     * Only one caller can modify the game at a time.
     */
    public void withWriteLock(UUID gameId, GameWriteOperation operation) throws Exception {
        ReadWriteLock lock = locks.get(gameId);
        if (lock == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }

        lock.writeLock().lock();
        try {
            Game game = games.get(gameId);
            if (game == null) {
                throw new IllegalArgumentException("Game not found: " + gameId);
            }
            operation.execute(game);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Execute a read operation on a game.
     */
    public <T> T withReadLock(UUID gameId, GameReadOperation<T> operation) throws Exception {
        ReadWriteLock lock = locks.get(gameId);
        if (lock == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }

        lock.readLock().lock();
        try {
            Game game = games.get(gameId);
            if (game == null) {
                throw new IllegalArgumentException("Game not found: " + gameId);
            }
            return operation.execute(game);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Remove a game from the registry (when finished and persisted).
     */
    public void remove(UUID gameId) {
        games.remove(gameId);
        locks.remove(gameId);
        log.info("Game removed from registry: {}", gameId);
    }

    /**
     * Get all active games (for admin/monitoring).
     */
    public List<Game> getAllActive() {
        return new ArrayList<>(games.values());
    }

    /**
     * Functional interface for write operations on a game.
     */
    @FunctionalInterface
    public interface GameWriteOperation {
        void execute(Game game) throws Exception;
    }

    /**
     * Functional interface for read operations on a game.
     */
    @FunctionalInterface
    public interface GameReadOperation<T> {
        T execute(Game game) throws Exception;
    }
}

