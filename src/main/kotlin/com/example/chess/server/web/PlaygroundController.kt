package com.example.chess.server.web

import com.example.chess.server.enums.GameMode
import com.example.chess.server.service.GameService
import com.example.chess.shared.Playground
import com.example.chess.shared.enums.Side
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/playground")
class PlaygroundController @Autowired constructor(
    private val gameService: GameService
) {

    //    @GetMapping("/{gameId}/playground/{position}")
    fun getArrangementByPosition(
        @PathVariable("gameId") gameId: Long,
        @PathVariable("position") position: Int
    ): Playground {

        val game = gameService.findAndCheckGame(gameId)
        val result = playgroundService.createArrangementByGame(game, position)

        if (game.mode == GameMode.AI && game.getPlayerSide() == Side.BLACK) {
            botService.applyBotMove(game, null)
        }
        return result
    }
}