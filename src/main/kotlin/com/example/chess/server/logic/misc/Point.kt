package com.example.chess.server.logic.misc

import com.example.chess.server.logic.IPoint
import com.example.chess.shared.Constants.BOARD_SIZE
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
            checkBoardIndices(rowIndex, columnIndex)
            return pointsPool[rowIndex][columnIndex]
        }

        fun of(dto: PointDTO) = of(dto.row, dto.col)

        fun of(compressedPoint: Int): Point {
            val row = compressedPoint shr COMPRESS_POINT_OFFSET
            val col = compressedPoint - (row shl COMPRESS_POINT_OFFSET)
            return of(row, col)
        }
    }

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