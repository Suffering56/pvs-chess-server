package com.example.chess.server.service

import com.example.chess.server.entity.Game
import com.example.chess.server.logic.IMutableChessboard
import com.example.chess.shared.enums.Side

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
interface IBotService {

    fun fireBotMoveSync(game: Game, botSide: Side, chessboard: IMutableChessboard)

    fun fireBotMoveAsync(game: Game, botSide: Side, chessboard: IMutableChessboard, delay: Long = 0)

    fun cancelBotMove(gameId: Long): Boolean
}