package com.example.chess.server.service.impl

import com.example.chess.server.App
import com.example.chess.server.entity.Game
import com.example.chess.server.entity.provider.EntityProvider
import com.example.chess.server.logic.*
import com.example.chess.server.logic.misc.isLongPawnMove
import com.example.chess.server.logic.misc.isPawnTransformation
import com.example.chess.server.logic.misc.toPrettyString
import com.example.chess.server.repository.GameRepository
import com.example.chess.server.repository.HistoryRepository
import com.example.chess.server.service.IGameService
import com.example.chess.server.service.IMovesProvider
import com.example.chess.shared.Constants.ROOK_LONG_COLUMN_INDEX
import com.example.chess.shared.Constants.ROOK_SHORT_COLUMN_INDEX
import com.example.chess.shared.dto.ChangesDTO
import com.example.chess.shared.enums.GameMode
import com.example.chess.shared.enums.PieceType
import com.example.chess.shared.enums.Side
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import kotlin.streams.toList

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
@Service
class GameService @Autowired constructor(
    private val gameRepository: GameRepository,
    private val historyRepository: HistoryRepository,
    private val entityProvider: EntityProvider,
    private val movesProvider: IMovesProvider,
    private val chessboardProvider: ChessboardProvider
) : IGameService {

    @Value("\${cache_spec.game:expireAfterAccess=1h,maximumSize=100,recordStats}")
    private lateinit var gameCacheSpec: String
    private lateinit var gameCache: LoadingCache<Long, Game>

    @PostConstruct
    private fun init() {
        gameCache = CacheBuilder.from(gameCacheSpec).build(
            CacheLoader.from<Long, Game> { gameId ->
                //TODO: очень важно, чтобы вес в кэше был настроен так, чтобы в приоритете
                //  было время обращения(т.е. только что рефрешнутый элемент обязательно остался в кэше)
                loadGameFromDatabase(gameId!!)
            }
        )
    }

    @Transactional
    override fun createNewGame(userId: String, mode: GameMode, side: Side, isConstructor: Boolean): Game {
        val game = gameRepository.save(entityProvider.createNewGame(userId, mode, side, isConstructor))

        return gameCache.asMap().compute(game.id!!) { gameId, existedGame ->
            require(existedGame == null) { "game(id=$gameId) already registered" }
            game
        }!!
    }

    @Transactional
    override fun registerPlayer(userId: String, gameId: Long, side: Side, forced: Boolean): IImmutableGame {
        return processGameSync(gameId) { game ->

            if (!App.DEBUG_ENABLED || !forced) {
                check(game.isSideEmpty(side)) { "cannot set game mode, because it already taken by another player" }
            }
            game.registerUser(userId, side)
            gameRepository.save(game)
        }
    }

    override fun findAndCheckGame(gameId: Long): Game {
        return gameCache.getUnchecked(gameId)
    }

    private fun processGameSync(gameId: Long, processHandler: (Game) -> Unit): Game {
        return gameCache.asMap().compute(gameId) { _, game ->
            requireNotNull(game) { "Game with id=$gameId not found" }
            processHandler.invoke(game)
            game
        }!!
    }

    private fun loadGameFromDatabase(gameId: Long): Game {
        return gameRepository.findById(gameId)
            .orElseThrow { IllegalArgumentException("Game with id=$gameId not found") }
    }

    override fun getMovesByPoint(game: IGame, chessboard: IChessboard, point: IPoint): Set<IPoint> {
        return movesProvider.getAvailableMoves(point, chessboard, game)
    }

    @Transactional
    override fun applyMove(game: Game, chessboard: IMutableChessboard, move: IMove): ChangesDTO {
        val piece = chessboard.getPiece(move.from)
        val availableMoves = getMovesByPoint(game, chessboard, move.from)

        require(availableMoves.contains(move.to)) {
            "cannot execute move=${move.toPrettyString(piece)}, because it not contains in available moves set: ${availableMoves.stream().map { it.toPrettyString() }.toList()}"
        }

        require(move.isPawnTransformation(piece) == (move.pawnTransformationPiece != null)) {
            "incorrect pawn transformation piece: ${move.pawnTransformationPiece}, expected: " +
                    "${move.pawnTransformationPiece?.let { "null" } ?: "not null"}, for move: ${move.toPrettyString(
                        piece
                    )}"
        }

        val initiatorSide = piece.side
        val enemySide = initiatorSide.reverse()
//        val sideFeatures = game.getSideFeatures(piece.side)
//        val enemySideFeatures = game.getSideFeatures(enemySide)

        val additionalMove = chessboard.applyMove(move)
        game.position = chessboard.position

        // очищаем стейт взятия на проходе, т.к. оно допустимо только в течении 1го раунда
        game.setPawnLongMoveColumnIndex(initiatorSide, null)

        val underCheck = movesProvider.isUnderCheck(enemySide, chessboard)
        game.setUnderCheck(initiatorSide, false) // мы не можем сделать такой ход, после которого окажемся под шахом
        game.setUnderCheck(enemySide, underCheck)           // но наш ход, может причинить шах противнику

        when (piece.type) {
            PieceType.KING -> {
                game.disableLongCastling(initiatorSide)
                game.disableShortCastling(initiatorSide)
            }
            PieceType.ROOK -> {
                if (move.from.col == ROOK_LONG_COLUMN_INDEX) {
                    game.disableLongCastling(initiatorSide)
                } else if (move.from.col == ROOK_SHORT_COLUMN_INDEX) {
                    game.disableShortCastling(initiatorSide)
                }
            }
            PieceType.PAWN -> {
                if (move.isLongPawnMove(piece)) {
                    game.setPawnLongMoveColumnIndex(initiatorSide, move.from.col)
                }
            }
            else -> {
                //do nothing
            }
        }

        val historyItem = entityProvider.createHistoryItem(game.id!!, chessboard.position, move, piece)

        gameRepository.save(game)   //sideFeatures сохранится вместе с game
        historyRepository.save(historyItem)

        return ChangesDTO(
            chessboard.position,
            move.toDTO(),
            additionalMove?.toDTO(),
            if (!underCheck) null else chessboard.getKingPoint(enemySide).toDTO()
        )
    }

    @Transactional
    override fun rollback(game: Game, positionsOffset: Int): Game {
        val newPosition = game.position - positionsOffset
        require(newPosition >= 0) { "position offset is too large" }

        game.position = newPosition
        historyRepository.removeAllByGameIdAndPositionGreaterThan(game.id!!, newPosition)

        return gameRepository.save(game)
    }

    override fun getNextMoveChanges(game: Game, prevMoveSide: Side, chessboardPosition: Int): ChangesDTO {
        val nextMoveHistory = historyRepository.findByGameIdAndPosition(game.id!!, chessboardPosition + 1)!!

        val chessboard = chessboardProvider.createChessboardForGame(game, chessboardPosition)
        val move = nextMoveHistory.toMove()

        val additionalMove = chessboard.applyMove(move)
        val isUnderCheck = movesProvider.isUnderCheck(prevMoveSide, chessboard)

        return ChangesDTO(
            chessboard.position,
            move.toDTO(),
            additionalMove?.toDTO(),
            if (isUnderCheck) chessboard.getKingPoint(prevMoveSide).toDTO() else null
        )
    }
}

data class GameWrapper(val gameId: Long)

fun load(gameId: Long) = GameWrapper(gameId)

fun main() {

    val poolSize = 10
    val executorService = Executors.newFixedThreadPool(poolSize)
    val gameCacheSpec = "expireAfterAccess=2s,maximumSize=4,recordStats"

    val gameCache = CacheBuilder.from(gameCacheSpec).build(
        CacheLoader.from<Long, GameWrapper> { gameId ->
            load(gameId!!)
        }
    )

//    val map = ConcurrentHashMap<Long, GameWrapper>()
    val map = gameCache.asMap()


//    for (i in 0..poolSize) {
//        executorService.submit {
//            map.compute(10) { k, v ->
//                println("k = ${k}")
//                Thread.sleep(1000)
//                println("v = ${v}")
//                null
//            }
//        }
//    }
//

    map.compute(1) { k, v ->
        val game = v ?: load(k)

        println("k1 = ${k}")
        Thread.sleep(1000)
        println("v1 = ${v}")
        println("v1g = ${game}")
        game
    }

//    gameCache.refresh(1)
//    gameCache.refresh(2)
//    gameCache.refresh(3)
//    gameCache.refresh(4)

//    executorService.submit {
//        map.compute(1) { k, v ->
//            println("k1 = ${k}")
//            Thread.sleep(1000)
//            println("v1 = ${v}")
//            GameWrapper(1)
//        }
//    }
//
//    executorService.submit {
//        map.compute(2) { k, v ->
//            println("k2 = ${k}")
//            println("v2 = ${v}")
//            GameWrapper(2)
//        }
//    }
//
//    map.compute(3) { k, v ->
//        println("k3 = ${k}")
//        println("v3 = ${v}")
//        GameWrapper(3)
//    }
//
//    map.compute(4) { k, v ->
//        println("k4 = ${k}")
//        println("v4 = ${v}")
//        GameWrapper(4)
//    }

    Thread.sleep(3300)


    println("gameCache.get(1) = ${gameCache.get(1)}")
    println("gameCache.get(1) = ${map.get(1)}")
//    println("gameCache.get(2) = ${gameCache.get(2)}")
//    println("gameCache.get(3) = ${gameCache.get(3)}")
//    println("gameCache.get(4) = ${gameCache.get(4)}")

    println("gameCache.size() = ${gameCache.size()}")

    executorService.shutdown()
    while (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
    }
    println("end")

}