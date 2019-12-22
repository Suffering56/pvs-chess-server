package com.example.chess.server.service.impl

import com.example.chess.server.entity.Game
import com.example.chess.server.entity.History
import com.example.chess.server.entity.provider.EntityProvider
import com.example.chess.server.logic.Chessboard
import com.example.chess.server.logic.IMutableChessboard
import com.example.chess.server.logic.misc.Point
import com.example.chess.server.repository.HistoryRepository
import com.example.chess.server.service.IChessboardProvider
import com.example.chess.shared.Constants.INITIAL_PIECES_COUNT
import com.example.chess.shared.enums.GameMode
import com.google.common.collect.Range
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * @author v.peschaniy
 *      Date: 24.07.2019
 */
@Component
class ChessboardProvider : IChessboardProvider {

    @Autowired
    private lateinit var historyRepository: HistoryRepository
    @Autowired
    private lateinit var entityProvider: EntityProvider

    override fun createChessboardForGame(game: Game, position: Int): IMutableChessboard {
        val availablePositionsRange = Range.closed(0, game.position)

        require(availablePositionsRange.contains(position)) { "position must be in range: $availablePositionsRange" }

        val history = historyRepository.findByGameIdAndPositionLessThanEqualOrderByPositionAsc(game.id!!, position)

        val isConstructorMovesPresent = history.any { it.isConstructor == true }
        require((game.mode == GameMode.CONSTRUCTOR) == isConstructorMovesPresent) { "strange state: game.mode: ${game.mode}, isConstructorMovesPresent: $isConstructorMovesPresent" }   //TODO temp check (или перетащить в if)

        if (game.mode == GameMode.CONSTRUCTOR) {
            require(history[0].position == INITIAL_PIECES_COUNT + 1) {
                //TODO temp check (или сделать предварительный чек на history.isEmpty)
                "wrong first recorded history item position: ${history[0].position}, expected: ${INITIAL_PIECES_COUNT + 1}"
            }
            return Chessboard.byHistory(createCleanHistory(game.id).asSequence() + history.asSequence())
        }

        return Chessboard.byHistory(history.asSequence())
    }

    private fun createCleanHistory(gameId: Long): List<History> {
        var position = 0
        val result: MutableList<History> = mutableListOf()

        for (row in sequenceOf(0, 1, 6, 7)) {
            for (col in 0..7) {
                val historyItem = entityProvider.createConstructorHistoryItem(
                    gameId,
                    ++position,
                    Point.of(row, col),
                    null
                )

                result.add(historyItem)
            }
        }

        return result
    }
}