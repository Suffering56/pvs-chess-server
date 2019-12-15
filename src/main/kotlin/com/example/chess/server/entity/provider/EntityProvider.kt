package com.example.chess.server.entity.provider

import com.example.chess.server.entity.Game
import com.example.chess.server.entity.GameFeatures
import com.example.chess.server.entity.History
import com.example.chess.server.logic.misc.toPrettyString
import com.example.chess.shared.api.IMove
import com.example.chess.shared.api.IPoint
import com.example.chess.shared.enums.GameMode
import com.example.chess.shared.enums.Piece
import com.example.chess.shared.enums.Side
import org.springframework.stereotype.Component

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
@Component
class EntityProvider {

    fun createNewGame(): Game {
        val gameFeatures = mutableMapOf<Side, GameFeatures>()
        val game = Game(
            null,
            0,
            GameMode.UNSELECTED,
            gameFeatures
        )

        gameFeatures[Side.WHITE] = gameFeatures(game = game, side = Side.WHITE)
        gameFeatures[Side.BLACK] = gameFeatures(game = game, side = Side.BLACK)

        return game
    }

    private fun gameFeatures(game: Game, side: Side) =
        GameFeatures(
            id = null,
            game = game,
            side = side,
            userId = null,
            lastVisitDate = null,
            longCastlingAvailable = true,
            shortCastlingAvailable = true,
            pawnLongMoveColumnIndex = null,
            isUnderCheck = false
        )

    fun createHistoryItem(gameId: Long, position: Int, move: IMove, pieceFrom: Piece): History {
        return History(
            id = null,
            gameId = gameId,
            position = position,
            rowIndexFrom = move.from.row,
            columnIndexFrom = move.from.col,
            rowIndexTo = move.to.row,
            columnIndexTo = move.to.col,
            pieceFromPawn = move.pawnTransformationPiece,
            description = move.toPrettyString(pieceFrom),
            isConstructor = null
        )
    }

    fun createConstructorHistoryItem(gameId: Long, position: Int, pointTo: IPoint, pieceFrom: Piece?): History {
        return History(
            id = null,
            gameId = gameId,
            position = position,
            rowIndexFrom = 0,
            columnIndexFrom = 0,
            rowIndexTo = pointTo.row,
            columnIndexTo = pointTo.col,
            pieceFromPawn = pieceFrom,
            description = "${pointTo.toPrettyString()}: ${pieceFrom?.shortName ?: "clean"}",
            isConstructor = true
        )
    }
}
