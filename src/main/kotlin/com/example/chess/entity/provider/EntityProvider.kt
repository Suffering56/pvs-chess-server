package com.example.chess.entity.provider

import com.example.chess.entity.Game
import com.example.chess.entity.GameFeatures
import com.example.chess.enums.GameMode
import com.example.chess.enums.Side
import org.springframework.stereotype.Component

@Component
class EntityProvider {

    fun createNewGame(): Game {
        val gameFeatures = mutableMapOf<Side, GameFeatures>()
        val game = Game(null, 0, GameMode.UNSELECTED, gameFeatures)

        gameFeatures[Side.WHITE] = gameFeatures(game = game, side = Side.WHITE)
        gameFeatures[Side.BLACK] = gameFeatures(game = game, side = Side.BLACK)

        return game
    }

    private fun gameFeatures(game: Game, side: Side) = GameFeatures(
        id = null,
        game = game,
        side = side,
        sessionId = null,
        lastVisitDate = null,
        longCastlingAvailable = true,
        shortCastlingAvailable = true,
        pawnLongMoveColumnIndex = null,
        isUnderCheck = false
    )

//    fun createHistoryItem(gameId: Long, position: Int, move: MoveDTO, pieceFrom: Piece): History {
//        return History(
//            id = null,
//            gameId = gameId,
//            position = position,
//            rowIndexFrom = move.rowIndexFrom,
//            columnIndexFrom = move.columnIndexFrom,
//            rowIndexTo = move.rowIndexTo,
//            columnIndexTo = move.columnIndexTo,
//            pieceFromPawn = move.pieceFromPawn,
//            description = move.createDescription(pieceFrom)
//        )
//    }
}