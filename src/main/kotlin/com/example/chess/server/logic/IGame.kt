package com.example.chess.server.logic

import com.example.chess.shared.enums.Side

interface IGame {

    fun getSideFeatures(side: Side): IGameFeatures
}
