package com.example.chess.server.logic.misc

import com.example.chess.server.logic.IUnmodifiableGame

data class GameResult<T>(
    val game: IUnmodifiableGame,
    val result: T
)