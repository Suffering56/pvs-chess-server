package com.example.chess.server.service

import com.example.chess.server.entity.ArrangementItem
import com.example.chess.server.entity.Game
import com.example.chess.server.logic.IMutableChessboard

/**
 * @author v.peschaniy
 *      Date: 24.07.2019
 */
interface IChessboardProvider {

    fun createChessboardForGame(game: Game, position: Int): IMutableChessboard

    fun createChessboardForGame(
        game: Game,
        position: Int = game.position,
        initialArrangement: Iterable<ArrangementItem>? = null
    ): IMutableChessboard
}