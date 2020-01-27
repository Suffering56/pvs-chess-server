package com.example.chess.server.entity.provider

import com.example.chess.server.entity.ArrangementItem
import com.example.chess.server.entity.Game
import com.example.chess.server.entity.GameFeatures
import com.example.chess.server.entity.HistoryItem
import com.example.chess.server.logic.misc.Move
import com.example.chess.server.logic.misc.Point
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
        //TODO: вообще лучше проверять расстановку и затем уже запрещать, если нужно (для конструктора)
        val castlingState = if (isConstructor) GameFeatures.ALL_CASTLING_DISABLED else GameFeatures.ALL_CASTLING_ENABLED

        gameFeatures[Side.WHITE] = gameFeatures(game, Side.WHITE, castlingState)
        gameFeatures[Side.BLACK] = gameFeatures(game, Side.BLACK, castlingState)

        game.registerUser(userId, side)
        return game
    }

    private fun defineInitialPosition(side: Side, isConstructor: Boolean): Int {
        if (!isConstructor) {
            return 0
        }

        /**
         * Пояснение к магическим числам:
         * 0 - это обычный игровой режим (игра была создана не в конструкторе и начинается со стандартной шахматной расстановки)
         *
         * 1 - нужно было любое число != 0, но такое чтоб Side.nextTurnSide(initialPosition) было = BLACK (игрок решил играть за черных, но ему предоставляется первый ход)
         * 2 - аналогично предыдущему пункту, только чтобы nextTurnSide было WHITE.
         *
         * Взяты минимальные подходящие числа, но в целом это могли быть любые положительные
         */
        return when (side) {
            Side.BLACK -> 1
            Side.WHITE -> 2
        }
    }

    private fun gameFeatures(game: Game, side: Side, castlingState: Int) =
        GameFeatures(
            id = null,
            game = game,
            side = side,
            userId = null,
            lastVisitDate = null,
            castlingState = castlingState,
            pawnLongMoveColumnIndex = null
        )

    fun createHistoryItem(gameId: Long, position: Int, move: Move, pieceFrom: Piece): HistoryItem {
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
