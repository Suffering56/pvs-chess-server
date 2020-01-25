package com.example.chess.server.logic.misc

import com.example.chess.shared.Constants.BOARD_SIZE
import com.example.chess.shared.dto.PointDTO
import com.example.chess.shared.enums.PieceType
import com.example.chess.shared.enums.PieceType.BISHOP
import com.example.chess.shared.enums.PieceType.ROOK
import kotlin.math.abs

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
class Point private constructor(
    val row: Int,
    val col: Int
) {
    companion object {

        private val pointsPool = Array(BOARD_SIZE) { rowIndex ->
            Array(BOARD_SIZE) { columnIndex ->
                Point(rowIndex, columnIndex)
            }
        }

        fun of(rowIndex: Int, columnIndex: Int): Point {
            checkBoardIndices(rowIndex, columnIndex)
            return pointsPool[rowIndex][columnIndex]
        }

        fun of(dto: PointDTO) = of(dto.row, dto.col)

        fun of(compressedPoint: Int): Point {
            val row = compressedPoint shr COMPRESS_POINT_OFFSET
            val col = compressedPoint - (row shl COMPRESS_POINT_OFFSET)
            return of(row, col)
        }

        private val columnNamesMap = mapOf(
            Pair(0, "h"),
            Pair(1, "g"),
            Pair(2, "f"),
            Pair(3, "e"),
            Pair(4, "d"),
            Pair(5, "c"),
            Pair(6, "b"),
            Pair(7, "a")
        )
    }

    fun compress() = compressPoint(this.row, this.col)

    fun isEqual(row: Int, col: Int): Boolean {
        return this.row == row && this.col == col
    }

    fun toDTO() = PointDTO(row, col)


    fun toPrettyString(): String {
        return "${columnNamesMap[col]}${row + 1}"
    }

    fun hasCommonVectorWith(other: Point): Boolean = hasCommonVectorWith(other.row, other.col)

    fun hasCommonVectorWith(other: Point, otherPieceType: PieceType): Boolean =
        hasCommonVectorWith(other.row, other.col, otherPieceType)

    fun hasCommonVectorWith(otherRow: Int, otherCol: Int): Boolean {
        return this.row == otherRow
                || this.col == otherCol
                || (abs(this.row - otherRow) == abs(this.col - otherCol))
    }

    fun hasCommonVectorWith(otherRow: Int, otherCol: Int, otherPieceType: PieceType): Boolean {
        return when (otherPieceType) {
            ROOK -> return this.row == otherRow || this.col == otherCol
            BISHOP -> abs(this.row - otherRow) == abs(this.col - otherCol)
            else -> throw UnsupportedOperationException("unsupported piece type: $otherPieceType")
        }
    }

    fun isBorderedWith(otherRow: Int, otherCol: Int): Boolean {
        check(this.row != otherRow || this.col != otherCol) {
            "is bordered with self? are you serious?, self: $this"
        }

        return abs(this.row - otherRow) <= 1
                && abs(this.col - otherCol) <= 1
    }

    fun isBorderedWith(other: Point) = isBorderedWith(other.row, other.col)


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other === null) return false
        if (other.javaClass != javaClass) throw UnsupportedOperationException("think about it")

        other as Point

        if (row != other.row) return false
        if (col != other.col) return false

        return true
    }

    override fun hashCode(): Int {
        return this.compress()
    }

    override fun toString(): String {
        return "[$row, $col]:${toPrettyString()}"
    }
}
