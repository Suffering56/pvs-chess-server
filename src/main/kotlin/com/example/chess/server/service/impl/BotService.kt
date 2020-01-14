package com.example.chess.server.service.impl

import com.example.chess.server.entity.Game
import com.example.chess.server.logic.IMutableChessboard
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

    private val taskMap: ConcurrentMap<Long, InternalTask> = ConcurrentHashMap()
    private val threadPool = Executors.newCachedThreadPool()

    internal data class InternalTask(
        val game: Game,
        val botSide: Side,
        val chessboard: IMutableChessboard
    )

    override fun fireBotMoveSync(game: Game, botSide: Side, chessboard: IMutableChessboard) {
        taskMap.computeIfAbsent(game.id) {
            processBotMove(game, botSide, chessboard)
            null
        }
    }

    override fun fireBotMoveAsync(game: Game, botSide: Side, chessboard: IMutableChessboard, delay: Long) {
        val gameId = game.id!!
        taskMap.putIfAbsent(gameId, InternalTask(game, botSide, chessboard))

        threadPool.submit {
            Thread.sleep(delay)

            taskMap.compute(gameId) { _, task ->
                if (task != null) {
                    processBotMove(task.game, task.botSide, task.chessboard)
                }
                null
            }
        }
    }

    override fun cancelBotMove(gameId: Long): Boolean {
        return taskMap.remove(gameId) != null
    }

    private fun processBotMove(game: Game, botSide: Side, chessboard: IMutableChessboard) {
        if (Side.nextTurnSide(game.position) == botSide) {
            applyFakeMove(game, chessboard)
        }
    }

    private fun applyFakeMove(game: Game, chessboard: IMutableChessboard) {
        val col = (game.position / 2)
        if (col < BOARD_SIZE) {
            gameService.applyMove(
                game,
                chessboard,
                Move(
                    Point.of(6, col),
                    Point.of(5, col),
                    null
                )
            )
        }
    }
}

