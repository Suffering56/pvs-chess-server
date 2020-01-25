package com.example.chess.server.logic

import com.example.chess.server.entity.ArrangementItem
import com.example.chess.server.entity.HistoryItem
import com.example.chess.server.logic.misc.*
import com.example.chess.shared.Constants.BOARD_SIZE
import com.example.chess.shared.Constants.ROOK_LONG_COLUMN_INDEX
import com.example.chess.shared.Constants.ROOK_SHORT_COLUMN_INDEX
import com.example.chess.shared.dto.CellDTO
import com.example.chess.shared.dto.ChessboardDTO
import com.example.chess.shared.dto.PointDTO
import com.example.chess.shared.enums.Piece
import com.example.chess.shared.enums.PieceType
import com.example.chess.shared.enums.Side
import java.util.*
import java.util.stream.Collectors
import java.util.stream.IntStream
import java.util.stream.Stream
import java.util.stream.StreamSupport

/**
 * @author v.peschaniy
 *      Date: 23.07.2019
 */
open class Chessboard private constructor(
    override var position: Int,
    private val flatMatrix: Array<Piece?>,
    private val kingPoints: MutableMap<Side, Point>
) : IChessboard {

    override fun getPieceNullable(compressedPoint: Int): Piece? = flatMatrix[compressedPoint]

    override fun getKingPoint(side: Side) = kingPoints[side]!!

    override fun cellsStream(side: Side): Stream<Cell> {
        return IntStream.range(0, POINTS_POOL_SIZE)
            .mapToObj { compressedPoint ->
                val piece = getPieceNullable(compressedPoint)
                if (piece != null) {
                    Cell.of(compressedPoint, piece)
                } else {
                    null
                }
            }
            .filter { it != null }
            .map { it!! }
            .filter { it.piece.side == side }
    }

    override fun applyMove(move: Move): Move? {
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

    private fun applyPawnTransformation(move: Move, pieceFrom: Piece) {
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

    private fun applyEnPassant(move: Move, pawn: Piece): Move? {
        val attackedPiece = requireNotNull(getPieceNullable(move.from.row, move.to.col)) {
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
        setPiece(move.from.row, move.to.col, null)

        return Move.cut(
            Point.of(move.from.row, move.to.col)
        )
    }

    private fun applyCastling(move: Move, king: Piece): Move? {
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

        val rook = getPieceNullable(rowIndex, rookColumnFrom)
        require(rook == Piece.of(king.side, PieceType.ROOK)) {
            "required castling rook not found: " +
                    "move=${move.toPrettyString(king)}\r\n${toPrettyString()}"
        }

        //move king
        applySimpleMove(move, king)
        //move rook
        setPiece(rowIndex, rookColumnFrom, null)
        setPiece(rowIndex, rookColumnTo, rook)

        return Move.of(
            Point.of(rowIndex, rookColumnFrom),
            Point.of(rowIndex, rookColumnTo),
            null
        )
    }

    private fun applySimpleMove(move: Move, pieceFrom: Piece) {
        flatMatrix[move.from.compress()] = null
        flatMatrix[move.to.compress()] = pieceFrom
    }

    override fun rollbackMove(move: Move, additionalMove: Move?, fallenPiece: Piece?) {
        val movedPiece = getPiece(move.to)

        if (movedPiece.isKing()) {
            kingPoints[movedPiece.side] = Point.of(move.from.toDTO())
        }

        additionalMove?.let {
            when (movedPiece.type) {
                PieceType.KING -> {
                    val rook = getPiece(additionalMove.to)
                    require(rook == Piece.of(movedPiece.side, PieceType.ROOK)) {
                        "incorrect additionalMove, expected piece.to=${Piece.of(
                            movedPiece.side,
                            PieceType.ROOK
                        )}, actual=$rook"
                    }

                    rollbackSimpleMove(it, Piece.of(movedPiece.side, PieceType.ROOK), null)
                }

                PieceType.PAWN -> {
                    require(it.isCut()) {
                        "incorrect additionalMove($it), expected cut-move"
                    }
                    rollbackEnPassant(it, movedPiece.side)
                }
                else -> throw IllegalStateException(
                    "additional move used only for en-passant or castling moves: move=$move, movedPiece=$movedPiece"
                )
            }
        }
        rollbackSimpleMove(move, movedPiece, fallenPiece)
        position--
    }

    private fun rollbackSimpleMove(move: Move, movedPiece: Piece, fallenPiece: Piece?) {
        flatMatrix[move.from.compress()] = movedPiece
        flatMatrix[move.to.compress()] = fallenPiece
    }

    private fun rollbackEnPassant(move: Move, moverSide: Side) {
        flatMatrix[move.from.compress()] = Piece.of(moverSide.reverse(), PieceType.PAWN)
    }

    private fun setPiece(row: Int, col: Int, piece: Piece?) {
        val index = compressPoint(row, col)
        flatMatrix[index] = piece
    }

    override fun toDTO(): ChessboardDTO {
        val matrixDto = Array(BOARD_SIZE) row@{ rowIndex ->

            Array(BOARD_SIZE) cell@{ columnIndex ->
                val piece = getPieceNullable(rowIndex, columnIndex)
                CellDTO(PointDTO(rowIndex, columnIndex), piece)
            }
        }

        return ChessboardDTO(position, matrixDto, null, null)
    }

    override fun copyOf(): IChessboard = Chessboard(position, flatMatrix.copyOf(), EnumMap(kingPoints))

    companion object {

        /**
         * Создает chessboard на основе переданной истории ходов(которая может быть пустой)
         *      и начальной расстановки initialArrangement,
         *      которая может быть null (в таком случае используется расстановка по умолчанию)
         */
        fun create(
            initialPosition: Int,
            historyItem: Sequence<HistoryItem>,
            initialArrangement: Iterable<ArrangementItem>? = null
        ): Chessboard {

            val arrangementMap = initialArrangement?.let {
                val map: Map<out Point, Piece> = StreamSupport.stream(it.spliterator(), false)
                    .collect(Collectors.toMap(
                        { arrangementItem -> Point.of(arrangementItem.row, arrangementItem.col) },
                        { arrangementItem -> arrangementItem.piece }
                    ))
                map
            }

            val chessboard = generate(initialPosition, initialChessboardGenerator(arrangementMap))
            historyItem.forEach { chessboard.applyMove(it.toMove()) }
            return chessboard
        }

        private fun generate(position: Int, pieceGenerator: (Int, Int) -> Piece?): Chessboard {
            val kingPoints = EnumMap<Side, Point>(Side::class.java)

            val matrix = Array(POINTS_POOL_SIZE) row@{ compressedPoint ->
                val point = Point.of(compressedPoint)
                val piece = pieceGenerator(point.row, point.col)

                if (piece != null && piece.isKing()) {
                    kingPoints[piece.side] = point
                }

                piece
            }

            return Chessboard(position, matrix, kingPoints)
        }

        private fun initialChessboardGenerator(initialArrangement: Map<out Point, Piece>?) = gen@{ row: Int, col: Int ->
            initialArrangement?.let {
                return@gen it[Point.of(row, col)]
            }

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