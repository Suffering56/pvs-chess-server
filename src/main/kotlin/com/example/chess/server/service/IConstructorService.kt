package com.example.chess.server.service

import com.example.chess.server.entity.Game
import com.example.chess.shared.dto.ChangesDTO
import com.example.chess.shared.dto.ChessboardDTO
import com.example.chess.shared.enums.Side

interface IConstructorService {

    fun initArrangement(game: Game, side: Side, clientChessboard: ChessboardDTO): ChangesDTO
}