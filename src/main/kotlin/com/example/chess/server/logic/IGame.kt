package com.example.chess.server.logic

import com.example.chess.shared.enums.Side

interface IGame : IUnmodifiableGame {

    fun setPawnLongMoveColumnIndex(side: Side, index: Int?)

    fun disableShortCastling(side: Side)

    fun disableLongCastling(side: Side)

    fun enableShortCastling(side: Side)

    fun enableLongCastling(side: Side)
}
