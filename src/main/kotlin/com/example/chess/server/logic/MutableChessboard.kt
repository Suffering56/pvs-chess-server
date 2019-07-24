package com.example.chess.server.logic

import com.example.chess.server.logic.misc.Point
import com.example.chess.shared.enums.Piece
import com.example.chess.shared.enums.Side

/**
 * @author v.peschaniy
 *      Date: 24.07.2019
 */
class MutableChessboard(
    position: Int,
    matrix: Array<Array<Piece?>>,
    kingPoints: Map<Side, Point>

) : Chessboard(position, matrix, kingPoints), IMutableChessboard {

}