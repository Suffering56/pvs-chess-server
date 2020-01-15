package com.example.chess.server.service

import com.example.chess.server.logic.IUnmodifiableGame
import com.example.chess.shared.dto.ChessboardDTO
import com.example.chess.shared.dto.ConstructorGameDTO
import com.example.chess.shared.enums.Side

interface IConstructorService {

    fun initArrangement(game: IUnmodifiableGame, side: Side, clientChessboard: ChessboardDTO): ConstructorGameDTO
}