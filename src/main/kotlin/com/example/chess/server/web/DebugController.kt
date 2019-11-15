package com.example.chess.server.web

import com.example.chess.server.App
import com.example.chess.server.service.impl.GameService
import com.example.chess.shared.dto.GameDTO
import com.example.chess.shared.enums.Side
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
@RestController
@RequestMapping("/api/debug")
class DebugController {

    @Autowired
    private lateinit var gameService: GameService

    @GetMapping("/version")
    fun get() = "version=${App.getVersion()}"

    /**
     * Принудительная регистрация игрока с указанным userId в партии с указанным gameId за сторону side
     */
    @GetMapping("/{gameId}/register/{side}/user/{userId}")
    fun registerPlayerForced(
        @PathVariable("gameId") gameId: Long,
        @PathVariable("side") side: Side,
        @PathVariable("userId") userId: String
    ): GameDTO {

        val game = gameService.findAndCheckGame(gameId)

        //проставляем userId принудительно
        game.registerUser(side, userId)
        return gameService.saveGame(game).toDTO(userId)
    }
}