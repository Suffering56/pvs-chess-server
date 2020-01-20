package com.example.chess.server.service.impl

import com.example.chess.server.logic.IChessboard
import com.example.chess.server.logic.IMove
import com.example.chess.server.logic.IUnmodifiableChessboard
import com.example.chess.server.logic.IUnmodifiableGame
import com.example.chess.server.logic.misc.Move
import com.example.chess.server.logic.misc.Point
import com.example.chess.server.service.IBotService
import com.example.chess.shared.Constants.BOARD_SIZE
import com.example.chess.shared.enums.Side
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
@Service
class BotService : IBotService {

    @Autowired private lateinit var gameService: GameService
    @Autowired private lateinit var movesProvider: MovesProvider

    private val deferredBotMovesMap: ConcurrentMap<Long, Int> = ConcurrentHashMap()
    private val threadPool = Executors.newScheduledThreadPool(10)

    override fun fireBotMoveSync(gameId: Long, expectedGamePosition: Int) {
        deferredBotMovesMap.compute(gameId) { _, _ ->
            gameService.applyBotMove(gameId, expectedGamePosition, this)
            null
        }
    }

    override fun fireBotMoveAsync(gameId: Long, expectedGamePosition: Int, delay: Long) {
        // если для этого gameId уже был затриггерен (но еще не выполнен) ход, заменим данные более актуальными
        deferredBotMovesMap[gameId] = expectedGamePosition

        threadPool.schedule({
            deferredBotMovesMap.computeIfPresent(gameId) { deferredGameId, expectedGamePosition ->
                gameService.applyBotMove(deferredGameId, expectedGamePosition, this)
                null
            }
        }, delay, TimeUnit.MILLISECONDS)
    }

    //будет вызываться под локом игры
    override fun invoke(game: IUnmodifiableGame, originalChessboard: IUnmodifiableChessboard): IMove {
        val botSide = game.getAndCheckBotSide()
        val chessboard = originalChessboard.copyOf()

        return createFakeMove(game, chessboard, botSide)
    }

    override fun cancelBotMove(gameId: Long) {
        deferredBotMovesMap.remove(gameId)
    }

    private fun createFakeMove(game: IUnmodifiableGame, chessboard: IChessboard, botSide: Side): Move {
        val col = (game.position / 2)
        check(col < BOARD_SIZE) { "game.position too large for this fake implementation" }

        return Move(
            Point.of(6, col),
            Point.of(5, col),
            null
        )
    }
}

