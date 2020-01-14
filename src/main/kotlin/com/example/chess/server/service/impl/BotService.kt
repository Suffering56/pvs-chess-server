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
import java.util.concurrent.*

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
    private val taskQueue: ConcurrentLinkedQueue<Long> = ConcurrentLinkedQueue()

    internal data class InternalTask(
        val game: Game,
        val botSide: Side,
        val chessboard: IMutableChessboard
    )

    override fun fireBotMoveSync(game: Game, botSide: Side, chessboard: IMutableChessboard) {
        taskMap.computeIfAbsent(game.id) {
            InternalTask(game, botSide, chessboard)
        }

        synchronized(game) {
            if (processingGameIds.add(game.id!!)) {
                try {
                    processBotMove(game, botSide, chessboard)
                } finally {
                    processingGameIds.remove(game.id)
                }
            }
        }
    }

    override fun fireBotMoveAsync(game: Game, botSide: Side, chessboard: IMutableChessboard) {
        Thread {
            Thread.sleep(3000)
            fireBotMoveAsync(game, botSide, chessboard)
        }
    }

    private fun processBotMove(game: Game, botSide: Side, chessboard: IMutableChessboard) {
//        chessboard.piecesStream(botSide)

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

