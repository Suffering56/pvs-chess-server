package com.example.chess.server.logic

import com.example.chess.server.entity.History
import com.example.chess.server.logic.misc.*
import com.example.chess.shared.ArrayTable
import com.example.chess.shared.Constants.BOARD_SIZE
import com.example.chess.shared.Constants.ROOK_LONG_COLUMN_INDEX
import com.example.chess.shared.Constants.ROOK_SHORT_COLUMN_INDEX
import com.example.chess.shared.api.IMove
import com.example.chess.shared.dto.CellDTO
import com.example.chess.shared.dto.ChessboardDTO
import com.example.chess.shared.dto.PointDTO
import com.example.chess.shared.enums.Piece
import com.example.chess.shared.enums.PieceType
import com.example.chess.shared.enums.Side

/**
 * @author v.peschaniy
 *      Date: 23.07.2019
 */
open class Chessboard private constructor(
    override var position: Int,
    private val matrix: ArrayTable<Piece?>,
    private val kingPoints: MutableMap<Side, Point>
) : IMutableChessboard {

    override fun getKingPoint(side: Side) = kingPoints[side]!!

    override fun getPieceNullable(rowIndex: Int, columnIndex: Int) = matrix[rowIndex][columnIndex]

    override fun applyMove(move: IMove): IMove? {
        if (move is ConstructorMove) {
            applyConstructorMove(move)
            position++
            return null
        }

        val pieceFrom = getPiece(move.from)

        if (pieceFrom.isKing()) {
            kingPoints[pieceFrom.side] = Point.of(move.to.toDTO())
        }

        val additionalMove = when {
            move.isCastling(pieceFrom) -> applyCastling(move, pieceFrom)
            move.isPawnTransformation(pieceFrom) -> {
                applyPawnTransformation(move, pieceFrom)
                null
            }
            move.isEnPassant(pieceFrom, getPieceNullable(move.to)) -> applyEnPassant(move, pieceFrom)
            else -> {
                applySimpleMove(move, pieceFrom)
                null
            }
        }

        position++
        return additionalMove
    }

    private fun applyPawnTransformation(move: IMove, pieceFrom: Piece) {
        val transformationPiece =
            requireNotNull(move.pawnTransformationPiece) {
                "transformation piece cannot be null: " +
                        "move=${move.toPrettyString(pieceFrom)}\r\n${toPrettyString()}"
            }

        require(pieceFrom.side == transformationPiece.side) {
            "wrong pawn transformation piece side: ${transformationPiece.side}, expected: ${pieceFrom.side}"
        }
        applySimpleMove(move, transformationPiece)
    }

    private fun applyEnPassant(move: IMove, pawn: Piece): IMove? {
        val attackedPiece = requireNotNull(matrix[move.from.row][move.to.col]) {
            "attacked piece[en passant] cannot be null: " +
                    "move=${move.toPrettyString(pawn)}\r\n${toPrettyString()}"
        }
        require(pawn.side != attackedPiece.side) {
            "attacked piece side must be opposite to the side of moved pawn:" +
                    "move=${move.toPrettyString(pawn)}\r\n${toPrettyString()}"
        }

        //move pawn
        applySimpleMove(move, pawn)
        //cut attacked piece
        matrix[move.from.row][move.to.col] = null

        return Move.cut(
            Point.of(move.from.row, move.to.col)
        )
    }

    private fun applyCastling(move: IMove, king: Piece): IMove? {
        require(move.from.row == move.to.row) { "castling row must be unchangeable: move=${move.toPrettyString(king)}" }

        val rowIndex = move.from.row
        val kingColumnFrom = move.from.col
        val rookColumnFrom: Int
        val rookColumnTo: Int

        if (move.isLongCastling(king)) {
            rookColumnFrom = ROOK_LONG_COLUMN_INDEX
            rookColumnTo = kingColumnFrom + 1
        } else {
            rookColumnFrom = ROOK_SHORT_COLUMN_INDEX
            rookColumnTo = kingColumnFrom - 1
        }

        val rook = matrix[rowIndex][rookColumnFrom]
        require(rook == Piece.of(king.side, PieceType.ROOK)) {
            "required castling rook not found: " +
                    "move=${move.toPrettyString(king)}\r\n${toPrettyString()}"
        }

        //move king
        applySimpleMove(move, king)
        //move rook
        matrix[rowIndex][rookColumnFrom] = null
        matrix[rowIndex][rookColumnTo] = rook

        return Move(
            Point.of(rowIndex, rookColumnFrom),
            Point.of(rowIndex, rookColumnTo),
            null
        )
    }

    private fun applySimpleMove(move: IMove, pieceFrom: Piece) {
        matrix[move.from.row][move.from.col] = null
        matrix[move.to.row][move.to.col] = pieceFrom
    }

    private fun applyConstructorMove(move: ConstructorMove) {
        matrix[move.to.row][move.to.col] = move.pieceFrom

        if (move.pieceFrom?.isKing() == true) {
            kingPoints[move.pieceFrom!!.side] = Point.of(move.to.toDTO())
        }
    }

    override fun toDTO(): ChessboardDTO {
        val matrixDto = Array(BOARD_SIZE) row@{ rowIndex ->
            val row = matrix[rowIndex]
            Array(BOARD_SIZE) cell@{ columnIndex ->
                val piece = row[columnIndex]
                CellDTO(PointDTO(rowIndex, columnIndex), piece)
            }
        }

        return ChessboardDTO(position, matrixDto, null, null)
    }

    companion object {

        /**
         * Создает chessboard на основе переданной истории ходов(которая может быть пустой)
         */
        fun byHistory(history: Sequence<History>): Chessboard {
            val chessboard = generate(0, initialChessboardGenerator())
            history.forEach { chessboard.applyMove(it.toMove()) }
            return chessboard
        }

        private fun generate(position: Int, pieceGenerator: (Int, Int) -> Piece?): Chessboard {
            val kingPoints = mutableMapOf<Side, Point>()

            val matrix = Array(BOARD_SIZE) row@{ row ->
                Array(BOARD_SIZE) cell@{ col ->

                    val piece = pieceGenerator(row, col)

                    if (piece != null && piece.isKing()) {
                        kingPoints[piece.side] = Point.of(row, col)
                    }

                    piece
                }
            }

            return Chessboard(position, matrix, kingPoints)
        }

        private fun initialChessboardGenerator() = gen@{ row: Int, col: Int ->
            val side = when (row) {
                0, 1 -> Side.WHITE
                6, 7 -> Side.BLACK
                else -> return@gen null
            }

            val pieceType = when (row) {
                1, 6 -> PieceType.PAWN
                else -> when (col) {
                    0, 7 -> PieceType.ROOK
                    1, 6 -> PieceType.KNIGHT
                    2, 5 -> PieceType.BISHOP
                    3 -> PieceType.KING
                    4 -> PieceType.QUEEN
                    else -> throw UnsupportedOperationException("incorrect col=$col")
                }
            }

            return@gen Piece.of(side, pieceType)
        }
    }

    override fun toPrettyString(): String {
        var result = "   a   b   c   d   e   f   g   h\r\n"
        result += "  ------------------------------\r\n"

        for (rowIndex in 7 downTo 0) {
            result += "${rowIndex + 1}| "
            for (columnIndex in 7 downTo 0) {
                val piece = getPieceNullable(Point.of(rowIndex, columnIndex))
                result += if (piece?.isPawn() == true) "P" else piece?.shortName ?: "."

                result += when (piece?.side) {
                    Side.WHITE -> "+"
                    Side.BLACK -> "-"
                    null -> " "
                }

                if (columnIndex != 0) {
                    result += "  "
                }
            }
            result += " |${rowIndex + 1}\r\n"
        }

        result += "  ------------------------------\n"
        result += "   a   b   c   d   e   f   g   h"

        return result
    }
}