package com.example.chess.server.web

import com.example.chess.server.enums.GameMode
import com.example.chess.server.service.IGameService
import com.example.chess.shared.dto.GameDTO
import com.example.chess.shared.enums.ExtendedSide
import com.example.chess.shared.enums.Side
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
@RestController
@RequestMapping("/api/init")
class InitController @Autowired constructor(
    private val gameService: IGameService
) {

    @GetMapping
    fun createGame(): GameDTO {
        val game = gameService.createNewGame()
        return game.toDTO()
    }

    @GetMapping("/{gameId}")
    fun getGame(
        @PathVariable("gameId") gameId: Long
    ): GameDTO {
        val game = gameService.findAndCheckGame(gameId)
        return game.toDTO()
    }

    @PostMapping("/{gameId}/mode/{gameMode}")
    @ResponseStatus(value = HttpStatus.OK)
    fun setMode(
        @PathVariable("gameId") gameId: Long,
        @PathVariable("gameMode") gameMode: GameMode
    ) {

        val game = gameService.findAndCheckGame(gameId)

        game.mode = gameMode
        gameService.saveGame(game)
    }

    @GetMapping("/{gameId}/side")
    fun getSideBySessionId(
        @PathVariable("gameId") gameId: Long,
        request: HttpServletRequest
    ): ExtendedSide {

        val game = gameService.findAndCheckGame(gameId)
        val sessionId = request.session.id

        var freeSlotsCount = 0
        var freeSide: Side? = null

        for (features in game.featuresMap.values) { //TODO: по хорошему бы скрыть реализацию featuresMap
            if (sessionId == features.sessionId) {
                //значит этот игрок уже начал эту игру ранее - позволяем ему продолжить за выбранную им ранее сторону
                return ExtendedSide.ofSide(features.side)
            }

            if (features.sessionId == null) {
                freeSlotsCount++
                freeSide = features.side
            }
        }

        if (freeSlotsCount == 2) {
            return ExtendedSide.UNSELECTED
        }

        //TODO: здесь нужно добавить проверку на то как давно пользователь был неактивен.

        return if (freeSide == null) {
            //нет свободных слотов - будет зрителем
            ExtendedSide.VIEWER
        } else {
            //игроку достается последний свободный слот
            return ExtendedSide.ofSide(freeSide)
        }
    }
}
