package com.example.chess.web

import com.example.chess.entity.Game
import com.example.chess.entity.provider.EntityProvider
import com.example.chess.enums.GameMode
import com.example.chess.repository.GameRepository
import com.example.chess.service.InitService
import com.example.chess.shared.PlayerSide
import com.example.chess.shared.enums.Side
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/api/init")
class InitController @Autowired constructor(
    private val entityProvider: EntityProvider,
    private val gameRepository: GameRepository,
    private val initService: InitService
) {

    @GetMapping
    fun createGame(): Game {
        val game = entityProvider.createNewGame()
        return gameRepository.save(game)
    }

    @GetMapping("/{gameId}")
    fun getGame(
        @PathVariable("gameId") gameId: Long,
        @RequestParam(value = "debug", required = false) isDebug: Boolean = false,
        @RequestParam(value = "desiredSide", required = false) desiredSide: Side,
        request: HttpServletRequest
    ): Game {

        val game = initService.findAndCheckGame(gameId)
        if (!isDebug) {
            return game
        }

        game.setSessionId(desiredSide, request.session.id)
        return gameRepository.save(game)
    }

    @PostMapping("/{gameId}/mode")
    @ResponseStatus(value = HttpStatus.OK)
    fun setMode(@PathVariable("gameId") gameId: Long, @RequestBody gameMode: GameMode): ResponseEntity<String> {

        val game = initService.findAndCheckGame(gameId)
        game.mode = gameMode

        gameRepository.save(game)

        return ResponseEntity.ok().build()
    }

    @GetMapping("/{gameId}/side")
    fun getSideBySessionId(@PathVariable("gameId") gameId: Long, request: HttpServletRequest): PlayerSide {

        val game = initService.findAndCheckGame(gameId)
        val sessionId = request.session.id

        var freeSlotsCount = 0
        var freeSide: Side? = null

        for (features in game.featuresMap.values) {
            if (sessionId == features.sessionId) {
                return PlayerSide.ofSide(features.side)
            }

            if (features.sessionId == null) {
                freeSlotsCount++
                freeSide = features.side
            }
        }

        if (freeSlotsCount == 2) {
            return PlayerSide.UNSELECTED
        }

        //TODO: здесь нужно добавить проверку на то как давно пользователь был неактивен.

        return if (freeSide == null) {
            //no free slots
            PlayerSide.VIEWER
        } else {
            //take free slot
            return PlayerSide.ofSide(freeSide)
        }
    }

    @PostMapping("/{gameId}/side")
    @ResponseStatus(value = HttpStatus.OK)
    fun setSide(@PathVariable("gameId") gameId: Long, @RequestBody selectedSide: PlayerSide, request: HttpServletRequest) {

        if (selectedSide == PlayerSide.SIDE_WHITE || selectedSide == PlayerSide.SIDE_BLACK) {
            val game = initService.findAndCheckGame(gameId)

            val side = selectedSide.toSide()
            game.setSessionId(side, request.session.id)
            game.setLastVisitDate(side, LocalDateTime.now())

            gameRepository.save(game)
        }
    }
//
//    @GetMapping("/{gameId}/arrangement/{position}")
//    @Throws(Exception::class)
//    fun getArrangementByPosition(
//        @PathVariable("gameId") gameId: Long,
//        @PathVariable("position") position: Int
//    ): ArrangementDTO {
//
//        val game = gameService.findAndCheckGame(gameId)
//        val result = gameService.createArrangementByGame(game, position)
//
//        if (game.mode === GameMode.AI && game.getPlayerSide() == Side.BLACK) {
//            botService.applyBotMove(game, null)
//        }
//        return result
//    }
}