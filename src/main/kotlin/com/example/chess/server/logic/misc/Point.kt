package com.example.chess.server.logic.misc

import com.example.chess.shared.Constants.BOARD_SIZE
import com.example.chess.shared.api.IPoint
import com.example.chess.shared.dto.PointDTO

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
class Point private constructor(
    override val row: Int,
    override val col: Int
) : IPoint {

    override fun toDTO() = PointDTO(row, col)

    companion object {

        private val pointsPool = Array(BOARD_SIZE) { rowIndex ->
            Array(BOARD_SIZE) { columnIndex ->
                Point(rowIndex, columnIndex)
            }
        }

        fun of(rowIndex: Int, columnIndex: Int): Point {
            checkBoardIndex(rowIndex) { "incorrect rowIndex=$rowIndex" }
            checkBoardIndex(columnIndex) { "incorrect columnIndex=$columnIndex" }

            return pointsPool[rowIndex][columnIndex]
        }

        fun of(dto: PointDTO) = of(dto.row, dto.col)

        private fun checkBoardIndex(index: Int, lazyMessage: () -> Any) {
            if (isIndexOutOfBounds(index)) {
                throw IndexOutOfBoundsException(lazyMessage().toString())
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Point

        if (row != other.row) return false
        if (col != other.col) return false

        return true
    }

    override fun hashCode(): Int {
        return this.compressToInt()
    }
}

//fun main() {
//    val x: Short = 1 shl 3
//    print(x)
//}
//
//TODO: возможно пригодится в будущем
//fun Int.toPoint(): Point {
//    val rowIndex = this shr Constants.POINT_OFFSET
//    val columnIndex = this - (rowIndex shl Constants.POINT_OFFSET)
//    return Point.of(rowIndex, columnIndex)
//}