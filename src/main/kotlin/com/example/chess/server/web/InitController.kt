package com.example.chess.server.web

import com.example.chess.server.core.Authorized
import com.example.chess.server.core.InjectGame
import com.example.chess.server.core.InjectUserId
import com.example.chess.server.entity.Game
import com.example.chess.server.service.IConstructorService
import com.example.chess.server.service.IGameService
import com.example.chess.shared.dto.ChessboardDTO
import com.example.chess.shared.dto.ConstructorGameDTO
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
    private val gameService: IGameService,
    private val constructorService: IConstructorService
) {

    @GetMapping("/new")
    fun createGame(
        @RequestParam userId: String,
        @RequestParam mode: GameMode,
        @RequestParam side: Side
    ): GameDTO {
        val game = gameService.createNewGame(userId, mode, side)
        return game.toDTO(userId)
    }

    @PostMapping("/constructor/new")
    fun createConstructedGame(
        @InjectUserId userId: String,
        @RequestParam mode: GameMode,
        @RequestParam side: Side,
        @RequestBody chessboard: ChessboardDTO
    ): ConstructorGameDTO {
        val game = gameService.createNewGame(userId, mode, side, true)
        return constructorService.initArrangement(game, side, chessboard)
    }

    @Authorized
    @GetMapping("/continue")
    fun getGame(
        @InjectGame game: Game,
        @InjectUserId userId: String
    ): GameDTO {
        return game.toDTO(userId)
    }

    @Authorized(false)   //TODO
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
