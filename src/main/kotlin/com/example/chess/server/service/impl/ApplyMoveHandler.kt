package com.example.chess.server.service.impl

import com.example.chess.server.logic.IChessboard
import com.example.chess.server.logic.IGame
import com.example.chess.server.logic.misc.Move
import com.example.chess.shared.Constants
import com.example.chess.shared.enums.PieceType
import org.springframework.stereotype.Component

@Component
class ApplyMoveHandler {

    fun applyMove(game: IGame, chessboard: IChessboard, move: Move): Move? {
        val pieceFrom = chessboard.getPiece(move.from)

        require(move.isPawnTransformation(pieceFrom) == (move.pawnTransformationPiece != null)) {
            "incorrect pawn transformation piece: ${move.pawnTransformationPiece}, expected: " +
                    "${move.pawnTransformationPiece?.let { "null" } ?: "not null"}, for move: ${move.toPrettyString(
                        pieceFrom
                    )}"
        }

        val initiatorSide = pieceFrom.side

        val additionalMove = chessboard.applyMove(move)
        game.position = chessboard.position

        // очищаем стейт взятия на проходе, т.к. оно допустимо только в течении 1го раунда
        game.setPawnLongMoveColumnIndex(initiatorSide, null)

        when (pieceFrom.type) {
            PieceType.KING -> {
                game.disableLongCastling(initiatorSide)
                game.disableShortCastling(initiatorSide)
            }
            PieceType.ROOK -> {
                if (move.from.col == Constants.ROOK_LONG_COLUMN_INDEX) {
                    game.disableLongCastling(initiatorSide)
                } else if (move.from.col == Constants.ROOK_SHORT_COLUMN_INDEX) {
                    game.disableShortCastling(initiatorSide)
                }
            }
            PieceType.PAWN -> {
                if (move.isLongPawnMove(pieceFrom)) {
                    game.setPawnLongMoveColumnIndex(initiatorSide, move.from.col)
                }
            }
            else -> {
                //do nothing
            }
        }

        return additionalMove
    }
}