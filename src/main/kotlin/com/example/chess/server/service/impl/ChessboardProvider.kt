package com.example.chess.server.service.impl

import com.example.chess.server.entity.ArrangementItem
import com.example.chess.server.logic.Chessboard
import com.example.chess.server.logic.IMutableChessboard
import com.example.chess.server.logic.IUnmodifiableGame
import com.example.chess.server.repository.ArrangementRepository
import com.example.chess.server.repository.HistoryRepository
import com.example.chess.server.service.IChessboardProvider
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
    private lateinit var arrangementRepository: ArrangementRepository

    override fun createChessboardForGame(game: IUnmodifiableGame, position: Int): IMutableChessboard {
        val gameId = requireGameId(game)
        val arrangement = if (game.initialPosition == 0) null
        else arrangementRepository.findAllByGameId(gameId)

        return createChessboardForGameWithArrangement(
            game,
            position,
            if (arrangement?.isEmpty() == false) arrangement else null
        )
    }

    override fun createChessboardForGameWithArrangement(
        game: IUnmodifiableGame,
        position: Int,
        initialArrangement: Iterable<ArrangementItem>?
    ): IMutableChessboard {

        val gameId = requireGameId(game)
        val availablePositionsRange = Range.closed(0, game.position)
        require(availablePositionsRange.contains(position)) { "position must be in range: $availablePositionsRange" }

        val history = historyRepository.findByGameIdAndPositionLessThanEqualOrderByPositionAsc(gameId, position)

        require((game.initialPosition != 0 == (initialArrangement != null))) {
            "strange state: game.initialPosition: ${game.initialPosition}, " +
                    "initialArrangement.isPresent: ${initialArrangement != null}"
        }

        return Chessboard.create(game.initialPosition, history.asSequence(), initialArrangement)
    }

    private fun requireGameId(game: IUnmodifiableGame) = requireNotNull(game.id) { "game.id is not presented, but required" }
}