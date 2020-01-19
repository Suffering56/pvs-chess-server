package com.example.chess.server.logic.session

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import javax.annotation.PostConstruct

@Component
class SyncManager {

    private val syncMap: ConcurrentHashMap<Long, GameLock> = ConcurrentHashMap()
    private val threadPool = Executors.newScheduledThreadPool(10)

    companion object {
        const val CLEAN_INTERVAL_MINUTES = 10L
        const val CLEAN_TIME_THRESHOLD = 60 * 60
    }

    @PostConstruct
    private fun init() {
        threadPool.scheduleWithFixedDelay({
            cleanSyncMap()
        }, CLEAN_INTERVAL_MINUTES, CLEAN_INTERVAL_MINUTES, TimeUnit.MINUTES)
    }

    private fun cleanSyncMap() {
        val currentTimeSec = currentTimeSec()
        syncMap.forEach { (gameId, lock) ->
            if (currentTimeSec - lock.lastExecutionTime > CLEAN_TIME_THRESHOLD) {
                syncMap.remove(gameId)
            }
        }
    }

    fun <T> executeLocked(gameId: Long, action: () -> T): T {
        val lock = getLock(gameId)

        try {
            lock.lock()
            return action.invoke()
        } finally {
            lock.unlock()
        }
    }

    fun <T> tryExecuteLocked(gameId: Long, action: () -> T): T {
        val lock = getLock(gameId)

        if (lock.tryLock()) {
            try {
                return action.invoke()
            } finally {
                lock.unlock()
            }
        } else {
            throw IllegalStateException("cannot acquire game($gameId) lock, because it is not free")
        }
    }

    private fun getLock(gameId: Long): GameLock {
        return syncMap.compute(gameId) { _, lock ->
            lock?.update() ?: GameLock()
        }!!
    }

    inner class GameLock(lock: ReentrantLock) : Lock by lock {
        var lastExecutionTime: Int = currentTimeSec()
            private set

        constructor() : this(ReentrantLock())

        fun update(): GameLock {
            lastExecutionTime = currentTimeSec()
            return this
        }
    }

    private fun currentTimeSec(): Int {
        return (System.currentTimeMillis() / 1000).toInt()
    }
}