package com.example.chess.server.web

import com.example.chess.server.service.IGameService
import com.example.chess.shared.dto.GameDTO
import com.example.chess.shared.enums.GameMode
import com.example.chess.shared.enums.Side
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
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

    @GetMapping("/continue")
    fun getGame(
        @RequestParam("userId") userId: String,
        @RequestParam("gameId") gameId: Long
    ): GameDTO {
        val game = gameService.findAndCheckGame(gameId)
        return game.toDTO(userId)
    }

    @PostMapping("/mode")
    @ResponseStatus(value = HttpStatus.OK)
    fun setMode(
        @RequestParam("userId") userId: String,
        @RequestParam("gameId") gameId: Long,
        @RequestParam("mode") mode: GameMode
    ): GameDTO {

        val game = gameService.findAndCheckGame(gameId)
        check(game.mode == GameMode.UNSELECTED) { "cannot set game mode, because it already defined" }

        game.mode = mode
        return gameService.saveGame(game).toDTO(userId)
    }

    @PostMapping("/side")
    @ResponseStatus(value = HttpStatus.OK)
    fun setSide(
        @RequestParam("userId") userId: String,
        @RequestParam("gameId") gameId: Long,
        @RequestParam("side") side: Side
    ): GameDTO {

        val game = gameService.findAndCheckGame(gameId)
        val sideFeatures = game.getSideFeatures(side)

        check(sideFeatures.sessionId == null) { "cannot set game mode, because it already taken by another player" }

        sideFeatures.sessionId = userId
        return gameService.saveGame(game).toDTO(userId)
    }

//    @GetMapping("/{gameId}/side")
//    fun getSideByUserId(
//        @PathVariable("gameId") gameId: Long,
//        @RequestParam("userId") userId: String
//    ): ExtendedSide {
//
//        val game = gameService.findAndCheckGame(gameId)
//
//        var freeSlotsCount = 0
//        var freeSide: Side? = null
//
//        for (features in game.featuresMap.values) { //TODO: по хорошему бы скрыть реализацию featuresMap
//            if (userId == features.sessionId) {
//                //значит этот игрок уже начал эту игру ранее - позволяем ему продолжить за выбранную им ранее сторону
//                return ExtendedSide.ofSide(features.side)
//            }
//
//            if (features.sessionId == null) {
//                freeSlotsCount++
//                freeSide = features.side
//            }
//        }
//
//        if (freeSlotsCount == 2) {
//            return ExtendedSide.UNSELECTED
//        }
//
//        //TODO: здесь нужно добавить проверку на то как давно пользователь был неактивен.
//
//        return if (freeSide == null) {
//            //нет свободных слотов - будет зрителем
//            ExtendedSide.VIEWER
//        } else {
//            //игроку достается последний свободный слот
//            return ExtendedSide.ofSide(freeSide)
//        }
//    }
}
