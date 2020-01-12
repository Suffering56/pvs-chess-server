package com.example.chess.server.entity.provider

import com.example.chess.server.entity.ArrangementItem
import com.example.chess.server.entity.Game
import com.example.chess.server.entity.GameFeatures
import com.example.chess.server.entity.HistoryItem
import com.example.chess.server.logic.misc.Point
import com.example.chess.server.logic.misc.toPrettyString
import com.example.chess.server.logic.IMove
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

    fun createNewGame(userId: String, mode: GameMode, side: Side, isConstructor: Boolean): Game {
        val gameFeatures = mutableMapOf<Side, GameFeatures>()
        val initialPosition = defineInitialPosition(side, isConstructor)

        val game = Game(
            null,
            initialPosition,
            mode,
            initialPosition,
            gameFeatures
        )

        gameFeatures[Side.WHITE] = gameFeatures(game = game, side = Side.WHITE)
        gameFeatures[Side.BLACK] = gameFeatures(game = game, side = Side.BLACK)

        if (isConstructor) {
            //TODO: вообще лучше проверять расстановку и затем уже запрещать, если нужно
            gameFeatures[Side.WHITE]?.disableCastling()
            gameFeatures[Side.BLACK]?.disableCastling()
        }

        game.registerUser(side, userId)
        return game
    }

    private fun defineInitialPosition(side: Side, isConstructor: Boolean): Int {
        if (!isConstructor) {
            return 0
        }

        return when (side) {
            Side.BLACK -> 1
            Side.WHITE -> 2
        }
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

    fun createHistoryItem(gameId: Long, position: Int, move: IMove, pieceFrom: Piece): HistoryItem {
        return HistoryItem(
            id = null,
            gameId = gameId,
            position = position,
            rowIndexFrom = move.from.row,
            columnIndexFrom = move.from.col,
            rowIndexTo = move.to.row,
            columnIndexTo = move.to.col,
            pieceFromPawn = move.pawnTransformationPiece,
            description = move.toPrettyString(pieceFrom)
        )
    }

    fun createConstructorArrangementItem(gameId: Long, point: Point, piece: Piece): ArrangementItem {
        return ArrangementItem(
            id = null,
            gameId = gameId,
            row = point.row,
            col = point.col,
            piece = piece
        )
    }
}
