package com.example.chess.server.web

import com.example.chess.server.core.Authorized
import com.example.chess.server.core.InjectGame
import com.example.chess.server.core.InjectUserId
import com.example.chess.server.entity.Game
import com.example.chess.server.service.IGameService
import com.example.chess.shared.dto.GameDTO
import com.example.chess.shared.enums.GameMode
import com.example.chess.shared.enums.Side
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
@RestController
@RequestMapping("/api/init")
class InitController @Autowired constructor(
    private val gameService: IGameService
) {

    @GetMapping("/new")
    fun createGame(): GameDTO {
        val game = gameService.createNewGame()
        return game.toDTO()
    }

    @Authorized
    @GetMapping("/continue")
    fun getGame(
        @InjectGame game: Game,
        @InjectUserId userId: String
    ): GameDTO {
        return game.toDTO(userId)
    }

    @Authorized
    @PostMapping("/mode")
    fun setMode(
        @InjectGame game: Game,
        @InjectUserId userId: String,
        @RequestParam("mode") mode: GameMode
    ): GameDTO {
        check(game.mode == GameMode.UNSELECTED) { "cannot set game mode, because it already defined" }

        game.mode = mode
        return gameService.saveGame(game).toDTO(userId)
    }

    @Authorized
    @PostMapping("/side")
    fun setSide(
        @InjectGame game: Game,
        @InjectUserId userId: String,
        @RequestParam("side") side: Side
    ): GameDTO {
        val sideFeatures = game.getSideFeatures(side)

        check(sideFeatures.userId == null) { "cannot set game mode, because it already taken by another player" }

        sideFeatures.userId = userId
        return gameService.saveGame(game).toDTO(userId)
    }
}
