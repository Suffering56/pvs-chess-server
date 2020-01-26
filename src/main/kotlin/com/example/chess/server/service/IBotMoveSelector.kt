package com.example.chess.server.service

import com.example.chess.server.logic.IChessboard
import com.example.chess.server.logic.IUnmodifiableGame
import com.example.chess.server.logic.misc.Move
import com.example.chess.shared.enums.Side

interface IBotMoveSelector {

    fun select(game: IUnmodifiableGame, chessboard: IChessboard, botSide: Side): Move
}