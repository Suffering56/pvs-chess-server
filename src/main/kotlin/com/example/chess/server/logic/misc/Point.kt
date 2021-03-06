package com.example.chess.server.logic.misc

import com.example.chess.shared.Constants.BOARD_SIZE
import com.example.chess.shared.dto.PointDTO
import com.example.chess.shared.enums.PieceType
import com.example.chess.shared.enums.PieceType.BISHOP
import com.example.chess.shared.enums.PieceType.ROOK
import com.google.common.collect.HashBiMap
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

        const val POINTS_POOL_SIZE = BOARD_SIZE * BOARD_SIZE
        val EMPTY_POINTS_LIST = emptyList<Point>()

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

        /**
         * Строка в формате e2/g6/a1/h8 и тд
         */
        fun of(str: String): Point {
            require(str.length == 2)

            val columnAlias = str.substring(0, 1)
            val rowAlias = str.substring(1).toInt()

            return of(rowAlias - 1, columnIndexByNameMap.get(columnAlias)!!)
        }

        private val columnNameByIndexMap = HashBiMap.create<Int, String>()

        init {
            columnNameByIndexMap[0] = "h"
            columnNameByIndexMap[1] = "g"
            columnNameByIndexMap[2] = "f"
            columnNameByIndexMap[3] = "e"
            columnNameByIndexMap[4] = "d"
            columnNameByIndexMap[5] = "c"
            columnNameByIndexMap[6] = "b"
            columnNameByIndexMap[7] = "a"
        }

        private val columnIndexByNameMap = columnNameByIndexMap.inverse()
    }

    fun compress() = compressPoint(this.row, this.col)

    fun isEqual(row: Int, col: Int): Boolean {
        return this.row == row && this.col == col
    }

    fun toDTO() = PointDTO(row, col)


    fun toPrettyString(): String {
        return "${columnNameByIndexMap[col]}${row + 1}"
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
            ROOK -> isOnSameVerticalOrHorizontal(otherRow, otherCol)
            BISHOP -> isOnSameDiagonal(otherRow, otherCol)
            else -> throw UnsupportedOperationException("unsupported piece type: $otherPieceType")
        }
    }

    fun isOnSameDiagonal(otherRow: Int, otherCol: Int) = abs(this.row - otherRow) == abs(this.col - otherCol)

    fun isOnSameDiagonal(otherPoint: Point) = isOnSameDiagonal(otherPoint.row, otherPoint.col)

    fun isOnSameVerticalOrHorizontal(otherRow: Int, otherCol: Int) = this.row == otherRow || this.col == otherCol

    fun isOnSameVerticalOrHorizontal(otherPoint: Point) = isOnSameVerticalOrHorizontal(otherPoint.row, otherPoint.col)

    fun isBorderedWith(otherRow: Int, otherCol: Int): Boolean {
        check(this.row != otherRow || this.col != otherCol) {
            "is bordered with self? are you serious?, self: $this"
        }

        return abs(this.row - otherRow) <= 1
                && abs(this.col - otherCol) <= 1
    }

    fun isBorderedWith(other: Point) = isBorderedWith(other.row, other.col)


    override fun equals(other: Any?): Boolean {
        return this === other
    }

    override fun hashCode(): Int {
        return this.compress()
    }

    override fun toString(): String {
        return "[$row, $col]:${toPrettyString()}"
    }
}