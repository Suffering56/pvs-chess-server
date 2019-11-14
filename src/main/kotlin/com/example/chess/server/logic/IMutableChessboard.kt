package com.example.chess.server.logic

import com.example.chess.shared.api.IMove

/**
 * @author v.peschaniy
 *      Date: 24.07.2019
 */
interface IMutableChessboard : IChessboard {

    fun applyMove(move: IMove)
}