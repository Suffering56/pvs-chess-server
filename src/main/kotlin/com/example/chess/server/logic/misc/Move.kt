package com.example.chess.server.logic.misc

import com.example.chess.shared.Constants.BOARD_SIZE
import com.example.chess.shared.Constants.ROOK_LONG_COLUMN_INDEX
import com.example.chess.shared.Constants.ROOK_SHORT_COLUMN_INDEX
import com.example.chess.shared.dto.MoveDTO
import com.example.chess.shared.enums.Piece
import org.eclipse.collections.api.tuple.Pair
import org.eclipse.collections.api.tuple.Twin
import org.eclipse.collections.impl.tuple.ImmutableEntry
import kotlin.math.abs

/**
 * @author v.peschaniy
 *      Date: 11.11.2019
 */
class Move private constructor(
    val from: Point,
    val to: Point,
    val pawnTransformationPiece: Piece?
) : Twin<Point> {

    companion object {

        private const val POINTS_TOTAL = BOARD_SIZE * BOARD_SIZE

        private val movesPool: Array<Array<Move>> = Array(POINTS_TOTAL) { compressedPointFrom ->
            val pointFrom = Point.of(compressedPointFrom)
            Array(POINTS_TOTAL) { compressedPointTo ->
                Move(pointFrom, Point.of(compressedPointTo), null)
            }
        }

        fun of(from: Point, to: Point): Move = movesPool[from.compress()][to.compress()]

        fun asTwinOf(from: Point, to: Point): Twin<Point> = of(from, to)

        fun of(from: Point, to: Point, pawnTransformationPiece: Piece?): Move {
            return if (pawnTransformationPiece == null) {
                of(from, to)
            } else {
                //TODO: cache it if needed
                println("new no-cached instance of Move created, because pawnTransformationPiece is $pawnTransformationPiece")
                Move(from, to, pawnTransformationPiece)
            }
        }

        fun of(move: MoveDTO): Move = of(Point.of(move.from), Point.of(move.to), move.pawnTransformationPiece)

        /**
         * строка в формате e2-e4, b1-f3
         */
        fun of(str: String): Move {
            require(str.length == 5)
            val split = str.split("-")
            require(split.size == 2)
            val from = split[0]
            val to = split[1]

            return of(Point.of(from), Point.of(to))
        }

        fun cut(point: Point): Move = of(point, point)
    }

    fun isCut() = from == to

    fun isPawnAttacks(pieceFrom: Piece): Boolean {
        return pieceFrom.isPawn() && from.col != to.col
    }

    fun isEnPassant(pieceFrom: Piece, pieceTo: Piece?): Boolean {
        return isPawnAttacks(pieceFrom) && pieceTo == null
    }

    fun isPawnTransformation(pieceFrom: Piece): Boolean {
        return pieceFrom.isPawn() && (to.row == ROOK_SHORT_COLUMN_INDEX || to.row == ROOK_LONG_COLUMN_INDEX)
    }

    fun isCastling(pieceFrom: Piece): Boolean {
        return pieceFrom.isKing() && abs(from.col - to.col) == 2
    }

    fun isLongCastling(pieceFrom: Piece): Boolean {
        return pieceFrom.isKing() && from.col - to.col == -2
    }

    fun isShortCastling(pieceFrom: Piece): Boolean {
        return pieceFrom.isKing() && from.col - to.col == 2
    }

    fun isLongPawnMove(pieceFrom: Piece): Boolean {
        return pieceFrom.isPawn() && from.col == to.col && abs(from.row - to.row) == 2
    }

    fun toPrettyString(pieceFrom: Piece): String {
        return "${pieceFrom.shortName}: ${from.toPrettyString()} -> ${to.toPrettyString()} (${pieceFrom.side})"
    }

    fun toDTO() = MoveDTO(from.toDTO(), to.toDTO(), pawnTransformationPiece)

    override fun swap() = of(to, from)

    override fun getOne() = from

    override fun getTwo() = to

    override fun put(map: MutableMap<Point, Point>) {
        map[from] = to
    }

    override fun toEntry(): ImmutableEntry<Point, Point> = ImmutableEntry.of(from, to)

    override fun compareTo(other: Pair<Point, Point>): Int {
        throw UnsupportedOperationException("compareTo unsupported in ${javaClass.canonicalName}")
    }
}