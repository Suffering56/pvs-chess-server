package com.example.chess.server.logic

import com.example.chess.shared.dto.GameDTO
import com.example.chess.shared.enums.GameMode
import com.example.chess.shared.enums.Side

interface IUnmodifiableGame {

    val id: Long?
    val position: Int
    val mode: GameMode
    val initialPosition: Int

    fun isUserRegistered(userId: String): Boolean

    fun isSideEmpty(side: Side): Boolean

    fun getUserSide(userId: String): Side?

    fun isShortCastlingAvailable(side: Side): Boolean

    fun isLongCastlingAvailable(side: Side): Boolean

    fun getPawnLongColumnIndex(side: Side): Int?

    fun toDTO(userId: String): GameDTO
}