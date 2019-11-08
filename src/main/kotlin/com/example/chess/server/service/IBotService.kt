package com.example.chess.server.service

import com.example.chess.server.entity.Game

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
interface IBotService {

    fun fireBotMove(game: Game, nothing: Nothing?)
}