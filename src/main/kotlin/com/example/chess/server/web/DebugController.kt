package com.example.chess.server.web

import com.example.chess.server.App
import com.example.chess.server.Destiny
import com.example.chess.server.service.IBotService
import com.example.chess.server.service.IGameService
import com.example.chess.shared.dto.GameDTO
import com.example.chess.shared.enums.GameMode
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

    @Autowired private lateinit var gameService: IGameService
    @Autowired private lateinit var botService: IBotService

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
        return gameService.registerPlayer(gameId, userId, side, true)
            .toDTO(userId)
    }

    @GetMapping("/test/{gameId}")
    fun testBotMove(@PathVariable("gameId") gameId: Long): String {
        val userId = "test"

        if (gameId == 0L) {
            val game = gameService.createNewGame(userId, GameMode.AI, Side.WHITE)

            gameService.applyPlayerMove(game.id!!, userId, Destiny.predictMove(0), 0)
            botService.fireBotMoveSync(game.id!!, 1)

            return game.id!!.toString()
        } else {
            val game = gameService.findAndCheckGame(gameId, userId).game
            val position = game.position

            gameService.applyPlayerMove(gameId, userId, Destiny.predictMove(position), position)
            botService.fireBotMoveSync(game.id!!, position + 1)

            return gameId.toString()
        }
    }
}