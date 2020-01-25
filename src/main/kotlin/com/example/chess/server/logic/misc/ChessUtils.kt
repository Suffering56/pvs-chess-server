package com.example.chess.server.logic.misc

import com.example.chess.shared.Constants
import com.example.chess.shared.enums.PieceType
import java.util.stream.IntStream
import java.util.stream.Stream

/**
 * @author v.peschaniy
 *      Date: 11.11.2019
 */

const val COMPRESS_POINT_OFFSET = 3
const val POINTS_POOL_SIZE = Constants.BOARD_SIZE shl COMPRESS_POINT_OFFSET



fun isIndexOutOfBounds(index: Int): Boolean {
    return index < 0 || index >= Constants.BOARD_SIZE
}


fun checkBoardIndices(rowIndex: Int, columnIndex: Int) {
    require(!isIndexOutOfBounds(rowIndex)) { "incorrect rowIndex=$rowIndex" }
    require(!isIndexOutOfBounds(columnIndex)) { "incorrect columnIndex=$columnIndex" }
}

fun boardPoints(): Stream<Point> {
    return IntStream.range(0, POINTS_POOL_SIZE)
        .mapToObj { Point.of(it) }
}

fun compressPoint(row: Int, col: Int): Int {
    checkBoardIndices(row, col)
    return (row shl COMPRESS_POINT_OFFSET) + col
}

