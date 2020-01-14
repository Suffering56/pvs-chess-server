package com.example.chess.server.logic

import com.example.chess.server.entity.GameFeatures
import com.example.chess.shared.enums.Side

interface IGame : IImmutableGame {

//    fun getSideFeatures(side: Side): GameFeatures

    fun setPawnLongMoveColumnIndex(side: Side, index: Int?)

    fun setUnderCheck(side: Side, isUnderCheck: Boolean)

    fun disableShortCastling(side: Side)

    fun disableLongCastling(side: Side)
}
