package com.example.chess.server.service.impl

import com.example.chess.server.logic.IChessboard
import com.example.chess.server.logic.IMove
import com.example.chess.server.logic.IUnmodifiableChessboard
import com.example.chess.server.logic.IUnmodifiableGame
import com.example.chess.server.logic.misc.Move
import com.example.chess.server.logic.misc.Point
import com.example.chess.server.service.IBotService
import com.example.chess.server.service.IGameService
import com.example.chess.shared.Constants.BOARD_SIZE
import com.example.chess.shared.enums.Side
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.Executors

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
@Service
class BotService @Autowired constructor(
    private val gameService: IGameService,
    private val movesProvider: MovesProvider
) : IBotService {

    private val syncMap: ConcurrentMap<Long, Any?> = ConcurrentHashMap()
    private val canceled: MutableSet<Long> = hashSetOf()
    private val threadPool = Executors.newCachedThreadPool()

    //будет вызываться под локом игры
    override fun invoke(game: IUnmodifiableGame, originalChessboard: IUnmodifiableChessboard): IMove {
        val botSide = game.getAndCheckBotSide()
        val chessboard = originalChessboard.copyOf()

        return createFakeMove(game, chessboard, botSide)
    }


    override fun fireBotMoveSync(gameId: Long) {

//        syncMap.putIfAbsent(gameId, gameId)

        syncMap.computeIfAbsent(gameId) {
            if (canceled.remove(gameId)) {
                return@computeIfAbsent null
            }

            gameService.applyBotMove(gameId, this)
            null
        }
    }

    override fun fireBotMoveAsync(gameId: Long, delay: Long) {
        threadPool.submit {
            Thread.sleep(delay)
            fireBotMoveSync(gameId)
        }
    }

    override fun cancelBotMove(gameId: Long): Boolean {
        canceled.add(gameId)    //а если она и не выполнялась, то тогда отменится ход который еще даже не зафейрился
        val stub = true
        return true
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

