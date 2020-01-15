package com.example.chess.server.service

import com.example.chess.server.entity.ArrangementItem
import com.example.chess.server.logic.IMutableChessboard
import com.example.chess.server.logic.IUnmodifiableGame

/**
 * @author v.peschaniy
 *      Date: 24.07.2019
 */
interface IChessboardProvider {

    fun createChessboardForGame(game: IUnmodifiableGame, position: Int = game.position): IMutableChessboard

    fun createChessboardForGameWithArrangement(
        game: IUnmodifiableGame,
        position: Int,
        initialArrangement: Iterable<ArrangementItem>? = null
    ): IMutableChessboard
}