package com.example.chess.server.logic

import com.example.chess.shared.enums.Side

interface IGame : IUnmodifiableGame {

    override var position: Int

    fun setPawnLongMoveColumnIndex(side: Side, index: Int?)

    fun disableShortCastling(side: Side)

    fun disableLongCastling(side: Side)

    fun setCastlingState(side: Side, castlingState: Int)

    fun registerUser(userId: String, side: Side)
}
