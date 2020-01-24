package com.example.chess.server.service

import com.example.chess.server.logic.IChessboard
import com.example.chess.server.logic.IMove
import com.example.chess.server.logic.IUnmodifiableGame
import com.example.chess.shared.enums.Side

interface IBotMoveSelector {

    fun selectRandom(game: IUnmodifiableGame, chessboard: IChessboard, botSide: Side): IMove
    fun selectBest(game: IUnmodifiableGame, chessboard: IChessboard, botSide: Side): IMove
}