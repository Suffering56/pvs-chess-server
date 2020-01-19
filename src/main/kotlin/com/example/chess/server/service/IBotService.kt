package com.example.chess.server.service

import com.example.chess.server.logic.IMove
import com.example.chess.server.logic.IUnmodifiableChessboard
import com.example.chess.server.logic.IUnmodifiableGame

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
interface IBotService : (IUnmodifiableGame, IUnmodifiableChessboard) -> IMove {

    fun fireBotMoveSync(gameId: Long, expectedGamePosition: Int)

    fun fireBotMoveAsync(gameId: Long, expectedGamePosition: Int, delay: Long = 1000L)
}