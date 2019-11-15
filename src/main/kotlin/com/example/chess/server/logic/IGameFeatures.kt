package com.example.chess.server.logic

interface IGameFeatures {

    val longCastlingAvailable: Boolean

    val shortCastlingAvailable: Boolean

    val pawnLongMoveColumnIndex: Int?

//    var isUnderCheck: Boolean
}
