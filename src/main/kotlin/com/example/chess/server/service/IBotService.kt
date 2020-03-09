package com.example.chess.server.service

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
interface IBotService {

    fun fireBotMoveSync(gameId: Long, expectedGamePosition: Int)

    fun fireBotMoveAsync(gameId: Long, expectedGamePosition: Int, delay: Long = 1000L)

    fun cancelBotMove(gameId: Long)
}