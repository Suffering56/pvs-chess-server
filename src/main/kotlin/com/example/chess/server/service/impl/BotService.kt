package com.example.chess.server.service.impl

import com.example.chess.server.entity.Game
import com.example.chess.server.logic.misc.Move
import com.example.chess.server.logic.misc.Point
import com.example.chess.server.service.IBotService
import com.example.chess.server.service.IChessboardProvider
import com.example.chess.server.service.IGameService
import com.example.chess.shared.enums.Side
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentSkipListSet

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
@Service
class BotService @Autowired constructor(
    private val gameService: IGameService,
    private val chessboardProvider: IChessboardProvider
) : IBotService {


    var processingGameIds: MutableSet<Long> = ConcurrentSkipListSet()

    override fun fireBotMove(game: Game, botSide: Side, nothing: Any?) {
        if (processingGameIds.add(game.id!!)) {
            try {
                if (Side.nextTurnSide(game.position) == botSide) {
                    if (game.position == 1) {
                        val chessboard = chessboardProvider.createChessboardForGame(game)
                        gameService.applyMove(
                            game,
                            chessboard,
                            Move(
                                Point.of(6, 3),
                                Point.of(4, 3),
                                null
                            )
                        )
                    }
                }
            } finally {
                processingGameIds.remove(game.id)
            }
        }
    }
}