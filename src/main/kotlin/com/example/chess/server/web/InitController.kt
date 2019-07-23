package com.example.chess.server.web

import com.example.chess.server.entity.Game
import com.example.chess.server.entity.provider.EntityProvider
import com.example.chess.server.enums.GameMode
import com.example.chess.server.repository.GameRepository
import com.example.chess.server.service.IGameService
import com.example.chess.shared.dto.GameDTO
import com.example.chess.shared.enums.ExtendedSide
import com.example.chess.shared.enums.Side
import com.google.common.base.Preconditions.checkState
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.lang.RuntimeException
import java.time.LocalDateTime
import javax.servlet.http.HttpServletRequest

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
@RestController
@RequestMapping("/api/init")
class InitController @Autowired constructor(
    private val entityProvider: EntityProvider,
    private val gameRepository: GameRepository,
    private val gameService: IGameService
) {

    @GetMapping
    fun createGame(): GameDTO {
        val game = gameRepository.save(entityProvider.createNewGame())
        return game.toDTO()
    }

    @GetMapping("/{gameId}")
    fun getGame(
        @PathVariable("gameId") gameId: Long,
        @RequestParam(value = "debug", required = false) isDebug: Boolean = false,
        @RequestParam(value = "desiredSide", required = false) desiredSide: Side?,
        request: HttpServletRequest
    ): GameDTO {

        val game = gameService.findAndCheckGame(gameId)

        //TODO: move to debug controller
        if (isDebug) {  //проставляем sessionId принудительно, чтобы продолжить игру после рестарта сервера (когда session.id клиента поменяется)
            checkNotNull(desiredSide) { "desiredSide is required in debug mode" }

            game.setSessionId(desiredSide, request.session.id)
            return gameRepository.save(game).toDTO()
        }
        return game.toDTO()
    }

    @PostMapping("/{gameId}/mode")
    @ResponseStatus(value = HttpStatus.OK)
    fun setMode(@PathVariable("gameId") gameId: Long, @RequestBody gameMode: GameMode) {

        val game = gameService.findAndCheckGame(gameId)

        game.mode = gameMode
        gameRepository.save(game)
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

        for (features in game.featuresMap.values) {
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
            //no free slots
            ExtendedSide.VIEWER
        } else {
            //take free slot
            return ExtendedSide.ofSide(freeSide)
        }
    }

    @PostMapping("/{gameId}/side")
    @ResponseStatus(value = HttpStatus.OK)
    fun setSide(
        @PathVariable("gameId") gameId: Long,
        @RequestBody selectedSide: ExtendedSide,
        request: HttpServletRequest
    ) {
        checkState(
            selectedSide == ExtendedSide.SIDE_WHITE || selectedSide == ExtendedSide.SIDE_BLACK,
            "incorrect PlayerSide value: $selectedSide"
        )

        val game = gameService.findAndCheckGame(gameId)

        val sessionId = request.session.id
        checkState(!game.isSessionRegistered(sessionId), "player(id=$sessionId) already chose his side")

        val side = selectedSide.toSide()
        checkState(game.getSideFeatures(side).sessionId == null, "side($side) already chosen by another player")

        game.setSessionId(side, sessionId)
        game.setLastVisitDate(side, LocalDateTime.now())
        gameRepository.save(game)
    }
}
