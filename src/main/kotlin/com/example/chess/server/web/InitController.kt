package com.example.chess.server.web

import com.example.chess.server.core.Authorized
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
    private val gameService: IGameService
) {

    @GetMapping("/new")
    fun createGame(
        @RequestParam userId: String,
        @RequestParam mode: GameMode,
        @RequestParam side: Side
    ): GameDTO {
        return gameService.createNewGame(userId, mode, side)
            .toDTO(userId)
    }

    @PostMapping("/constructor/new")
    fun createConstructedGame(
        @RequestParam userId: String,
        @RequestParam mode: GameMode,
        @RequestParam side: Side,
        @RequestBody chessboard: ChessboardDTO
    ): ConstructorGameDTO {
        return gameService.createConstructedGame(userId, mode, side, chessboard)
            .result
    }

    @PostMapping("/side")
    fun setSide(
        @RequestParam gameId: Long,
        @RequestParam userId: String,
        @RequestParam side: Side
    ): GameDTO {
        return gameService.registerPlayer(gameId, userId, side)
            .toDTO(userId)
    }

    @Authorized
    @GetMapping("/continue")
    fun continueGame(
        @RequestParam gameId: Long,
        @RequestParam userId: String
    ): GameDTO {
        return gameService.findAndCheckGame(gameId, userId)
            .game
            .toDTO(userId)
    }
}
