package com.example.chess.entity.provider

import com.example.chess.entity.Game
import com.example.chess.entity.GameFeatures
import com.example.chess.enums.GameMode
import com.example.chess.shared.enums.Side
import org.springframework.stereotype.Component

@Component
class GameProvider {

    fun createNewGame(): Game {
        val gameFeatures = mutableMapOf<Side, GameFeatures>()
        val game = Game(null, 0, GameMode.UNSELECTED, gameFeatures)

        gameFeatures[Side.WHITE] = gameFeatures(game = game, side = Side.WHITE)
        gameFeatures[Side.BLACK] = gameFeatures(game = game, side = Side.BLACK)

        return game
    }

    private fun gameFeatures(game: Game, side: Side) = GameFeatures(
        id = null,
        game = game,
        side = side,
        sessionId = null,
        lastVisitDate = null,
        longCastlingAvailable = true,
        shortCastlingAvailable = true,
        pawnLongMoveColumnIndex = null,
        isUnderCheck = false
    )
}