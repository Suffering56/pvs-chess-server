package com.example.chess.server.service.impl

import com.example.chess.server.App
import com.example.chess.server.entity.Game
import com.example.chess.server.entity.provider.EntityProvider
import com.example.chess.server.logic.*
import com.example.chess.server.logic.misc.*
import com.example.chess.server.logic.session.SyncManager
import com.example.chess.server.repository.ArrangementRepository
import com.example.chess.server.repository.GameRepository
import com.example.chess.server.repository.HistoryRepository
import com.example.chess.server.service.IBotService
import com.example.chess.server.service.IGameService
import com.example.chess.server.service.IMovesProvider
import com.example.chess.shared.Constants.ROOK_LONG_COLUMN_INDEX
import com.example.chess.shared.Constants.ROOK_SHORT_COLUMN_INDEX
import com.example.chess.shared.dto.ChangesDTO
import com.example.chess.shared.dto.ChessboardDTO
import com.example.chess.shared.dto.ConstructorGameDTO
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
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
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
    private val chessboardProvider: ChessboardProvider,
    private val syncManager: SyncManager,
    private val arrangementRepository: ArrangementRepository,
    private val botService: IBotService
) : IGameService {

    @Value("\${cache_spec.game:expireAfterAccess=1h,maximumSize=100,recordStats}")
    private lateinit var gameCacheSpec: String
    private lateinit var gameCache: LoadingCache<Long, Game>

    @PostConstruct
    private fun init() {
        gameCache = CacheBuilder.from(gameCacheSpec).build(
            CacheLoader.from<Long, Game> { gameId ->
                loadGameFromDatabase(gameId!!)
            }
        )
    }

    @Transactional
    override fun createNewGame(userId: String, mode: GameMode, side: Side): IUnmodifiableGame {
        return createNewGame(userId, mode, side, false)
    }

    @Transactional
    override fun registerPlayer(gameId: Long, userId: String, side: Side, forced: Boolean): IUnmodifiableGame {
        return processGameLocked(gameId, 0) { game ->

            if (!App.DEBUG_ENABLED || !forced) {
                check(game.isSideEmpty(side)) { "cannot set game mode, because it already taken by another player" }
            }
            game.registerUser(userId, side)

            saveGame(game)
        }.result
    }

    override fun checkRegistration(gameId: Long, userId: String) {
        require(getUnchecked(gameId).isUserRegistered(userId)) {
            "user(id=$userId) has no access to game=$gameId"
        }
    }

    // может вызываться для зрителя, поэтому game.getUserSide(userId) может вернуть null
    override fun findAndCheckGame(gameId: Long, userId: String, chessboardPosition: Int?): GameResult<IUnmodifiableChessboard> {
        val game = getUnchecked(gameId)

        if (game.position == game.initialPosition && game.mode == GameMode.AI) {
            val userSide = game.getUserSide(userId)

            if (userSide != null && userSide != Side.nextTurnSide(game.initialPosition)) {
                //первый ход в игре за ботом. нужно его пнуть, чтобы он начал партию
                botService.fireBotMoveAsync(gameId, 0)
            }
        }

        return GameResult(
            game,
            chessboardProvider.createChessboardForGame(game, chessboardPosition ?: game.position)
        )
    }

    @Transactional
    override fun applyPlayerMove(gameId: Long, userId: String, move: IMove, clientPosition: Int): GameResult<ChangesDTO> {
        //TODO: запретить ходить в пвп режиме, пока незарегистрированы оба игрока
        return processGameLocked(gameId, clientPosition) { game ->

            val playerSide = game.getUserSide(userId)
            val chessboard = chessboardProvider.createChessboardForGame(game)

            check(playerSide == Side.nextTurnSide(game.position)) {
                "not player turn! expected side: $playerSide, side by position: ${Side.nextTurnSide(game.position)}"
            }
            check(playerSide == chessboard.getPiece(move.from).side) {
                "not player pieceFrom! expected side: $playerSide, pieceFrom.side: ${chessboard.getPiece(move.from).side}"
            }

            val changes = applyMove(game, chessboard, move)

            if (game.mode == GameMode.AI) {
                botService.fireBotMoveAsync(gameId, changes.position)
            }

            changes
        }
    }

    @Transactional
    override fun applyBotMove(gameId: Long, clientPosition: Int, moveProvider: (IUnmodifiableGame, IUnmodifiableChessboard) -> IMove): GameResult<ChangesDTO> {
        return tryProcessGameLocked(gameId, clientPosition) { game ->

            val botSide = game.getAndCheckBotSide()
            val chessboard = chessboardProvider.createChessboardForGame(game)

            check(botSide == Side.nextTurnSide(game.position)) {
                "not bot turn! expected side: $botSide, side by position: ${Side.nextTurnSide(game.position)}"
            }

            val move = moveProvider.invoke(game, chessboard)

            check(botSide == chessboard.getPiece(move.from).side) {
                "not bot pieceFrom! expected side: $botSide, pieceFrom.side: ${chessboard.getPiece(move.from).side}"
            }

            applyMove(game, chessboard, move)
        }
    }

    @Transactional
    override fun rollback(gameId: Long, positionsOffset: Int, clientPosition: Int): GameResult<IUnmodifiableChessboard> {
        require(positionsOffset > 0) { "position offset must be positive" }

        return processGameLocked(gameId, clientPosition) { game ->

            val newPosition = game.position - positionsOffset
            require(newPosition >= game.initialPosition) { "position offset is too large" }

            game.position = newPosition

            historyRepository.removeAllByGameIdAndPositionGreaterThan(gameId, newPosition)
            saveGame(game)

            val chessboard = chessboardProvider.createChessboardForGame(game)

            if (game.mode == GameMode.AI) {
                botService.fireBotMoveAsync(gameId, game.position)
            }

            chessboard
        }
    }

    override fun getMovesByPoint(gameId: Long, point: IPoint, clientPosition: Int): GameResult<Set<IPoint>> {
        val game: IUnmodifiableGame = getUnchecked(gameId)
        checkPosition(game.position, clientPosition)

        val chessboard = chessboardProvider.createChessboardForGame(game)

        check(Side.nextTurnSide(game.position) == chessboard.getPiece(point).side) {
            "incorrect side of pointFrom. expected:${Side.nextTurnSide(game.position)}, " +
                    "found: ${chessboard.getPiece(point).side}"
        }

        return GameResult(
            game,
            movesProvider.getAvailableMoves(game, chessboard, point)
        )
    }

    override fun listenChanges(gameId: Long, userId: String, clientPosition: Int): GameResult<ChangesDTO> {
        val game: IUnmodifiableGame = getUnchecked(gameId)
        checkPosition(game.position, clientPosition)

        val clientSide = game.getUserSide(userId)!!

        check(Side.nextTurnSide(clientPosition) == clientSide.reverse()) {
            "incorrect clientPosition: $clientPosition, because it is not equals with next turn(opponent move) side : ${clientSide.reverse()}"
        }

        val nextMoveHistory = requireNotNull(historyRepository.findFirstByGameIdOrderByPositionDesc(gameId)) {
            "history not exist for game=$gameId"
        }

        val changes = if (nextMoveHistory.position == clientPosition + 1) {
            val chessboard = chessboardProvider.createChessboardForGame(game, clientPosition)
            val move = nextMoveHistory.toMove()

            val additionalMove = chessboard.applyMove(move)
            val isUnderCheck = movesProvider.isUnderCheck(clientSide, chessboard)

            ChangesDTO(
                chessboard.position,
                move.toDTO(),
                additionalMove?.toDTO(),
                if (isUnderCheck) chessboard.getKingPoint(clientSide).toDTO() else null
            )

        } else {
            require(nextMoveHistory.position == clientPosition) { "game synchronization error: clientPosition=$clientPosition, historyPosition=$nextMoveHistory.position" }
            ChangesDTO.EMPTY
        }

        return GameResult(game, changes)
    }

    @Transactional
    override fun createConstructedGame(userId: String, mode: GameMode, side: Side, clientChessboard: ChessboardDTO): GameResult<ConstructorGameDTO> {
        //TODO: validate(clientChessboard)

        val game = createNewGame(userId, mode, side, true)
        val gameId = game.id!!

        return processGameLocked(gameId, game.initialPosition) {
            val arrangement = Arrays.stream(clientChessboard.matrix)
                .flatMap { Arrays.stream(it) }
                .filter { it.piece != null }
                .map {
                    entityProvider.createConstructorArrangementItem(
                        gameId,
                        Point.of(it.pointDTO),
                        it.piece!!
                    )
                }
                .toList()

            val initialArrangement = arrangementRepository.saveAll(arrangement)

            val serverChessboard = chessboardProvider.createChessboardForGameWithArrangement(game, game.position, initialArrangement)
            val underCheck = movesProvider.isUnderCheck(side.reverse(), serverChessboard)

            ConstructorGameDTO(
                gameId,
                game.position,
                serverChessboard.toDTO().matrix,
                if (!underCheck) null else serverChessboard.getKingPoint(side.reverse()).toDTO()
            )
        }
    }

    private fun applyMove(game: Game, chessboard: IChessboard, move: IMove): ChangesDTO {
        val pieceFrom = chessboard.getPiece(move.from)
        val availableMoves = movesProvider.getAvailableMoves(game, chessboard, move.from)

        require(availableMoves.contains(move.to)) {
            "cannot execute move=${move.toPrettyString(pieceFrom)}, because it not contains in available moves set: " +
                    "${availableMoves.stream().map { it.toPrettyString() }.toList()}"
        }

        require(move.isPawnTransformation(pieceFrom) == (move.pawnTransformationPiece != null)) {
            "incorrect pawn transformation piece: ${move.pawnTransformationPiece}, expected: " +
                    "${move.pawnTransformationPiece?.let { "null" } ?: "not null"}, for move: ${move.toPrettyString(
                        pieceFrom
                    )}"
        }

        val initiatorSide = pieceFrom.side
        val enemySide = initiatorSide.reverse()

        val additionalMove = chessboard.applyMove(move)
        game.position = chessboard.position

        // очищаем стейт взятия на проходе, т.к. оно допустимо только в течении 1го раунда
        game.setPawnLongMoveColumnIndex(initiatorSide, null)

        val underCheck = movesProvider.isUnderCheck(enemySide, chessboard)
        game.setUnderCheck(initiatorSide, false)    // мы не можем сделать такой ход, после которого окажемся под шахом
        game.setUnderCheck(enemySide, underCheck)               // но наш ход, может причинить шах противнику

        when (pieceFrom.type) {
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
                if (move.isLongPawnMove(pieceFrom)) {
                    game.setPawnLongMoveColumnIndex(initiatorSide, move.from.col)
                }
            }
            else -> {
                //do nothing
            }
        }

        val historyItem = entityProvider.createHistoryItem(game.id!!, chessboard.position, move, pieceFrom)
        historyRepository.save(historyItem)
        saveGame(game)

        return ChangesDTO(
            chessboard.position,
            move.toDTO(),
            additionalMove?.toDTO(),
            if (!underCheck) null else chessboard.getKingPoint(enemySide).toDTO()
        )
    }

    private fun createNewGame(userId: String, mode: GameMode, side: Side, isConstructor: Boolean): IUnmodifiableGame {
        synchronized(userId.intern()) {
            val newGame = entityProvider.createNewGame(userId, mode, side, isConstructor)   //TMP
            val game = gameRepository.save(newGame)                                         //TMP
            check(game === newGame) { "temp check" }                                   //TMP

//        val game = gameRepository.save(entityProvider.createNewGame(userId, mode, side, isConstructor))
            val gameId = game.id!!

            return syncManager.executeLocked(gameId) {
                gameCache.put(gameId, game)
                game
            }
        }
    }

    private fun saveGame(game: Game): Game {
        val savedGame = gameRepository.save(game)
        if (game != savedGame) {
            System.err.println("savedGame($savedGame) is not identity-equals with game($game)")
            gameCache.put(game.id!!, savedGame)
        }
        return savedGame
    }

    private fun <T> processGameLocked(gameId: Long, clientPosition: Int, processHandler: (Game) -> T): GameResult<T> {
        return syncManager.executeLocked(gameId) {
            processGame(gameId, clientPosition, processHandler)
        }
    }

    private fun <T> tryProcessGameLocked(gameId: Long, clientPosition: Int, processHandler: (Game) -> T): GameResult<T> {
        return syncManager.tryExecuteLocked(gameId) {
            processGame(gameId, clientPosition, processHandler)
        }
    }

    private fun <T> processGame(gameId: Long, clientPosition: Int, processHandler: (Game) -> T): GameResult<T> {
        val game = getUnchecked(gameId)
        checkPosition(game.position, clientPosition)

        val result = processHandler.invoke(game)

        return GameResult(
            game,
            result
        )
    }

    private fun checkPosition(serverPosition: Int, clientPosition: Int) {
        require(serverPosition == clientPosition) { "game synchronization error: clientPosition=$clientPosition, serverPosition=$serverPosition" }
    }

    private fun loadGameFromDatabase(gameId: Long): Game {
        return gameRepository.findById(gameId)
            .orElseThrow { IllegalArgumentException("Game with id=$gameId not found") }
    }

    private fun getUnchecked(gameId: Long): Game = gameCache.getUnchecked(gameId)
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

    val lock = ReentrantLock()

    lock.lock()
    println("lock 1")
    lock.lock()
    println("lock 2")
    lock.lock()
    println("lock 3")
    lock.lock()
    println("lock 4")

    lock.unlock()
    println("unlock 1")

//    executorService.submit {
//        lock.lock()
//        println("lock 1")
//        Thread.sleep(3000)
//        lock.unlock()
//        println("unlock 1")
//    }
//
//    executorService.submit {
//        Thread.sleep(1000)
//        lock.lockInterruptibly()
//        println("lockInterruptibly 2")
//        lock.unlock()
//        println("unlock 2")
//    }

//    executorService.submit {
//        Thread.sleep(1000)
//        if (lock.tryLock()) {
//            println("lock 2")
//            lock.unlock()
//            println("unlock 2")
//        } else {
//            println("cant lock 2")
//        }
//    }
//    println("execute locked")
//    println("lock.tryLock() = ${lock.tryLock()}")
//
//    lock.unlock()
//    executorService.shutdown()
//    while (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
//    }
//    println("end")
//    return

//    val map = ConcurrentSkipListMap<Long, GameWrapper>()
    val map = ConcurrentHashMap<Long, GameWrapper>()

    map[1] = load(1)
    map[2] = load(2)
    map[3] = load(3)
    map[4] = load(4)
//    val map = gameCache.asMap()

    executorService.submit {
        println("submit 1")

        map.compute(2) { _, _ ->
            //            map.put(1, load(2))
//            println("after put in compute")
            Thread.sleep(3000)
            println("compute 1")
            load(2)
        }
    }
//
    executorService.submit {
        println("submit 2")
        Thread.sleep(1100)

        map.forEach { (k, v) ->
            println("remove: $k")
            map.remove(k)
        }


//        map.compute(2) { _, _ ->
//            println("compute 2")
//            load(2)
//        }
    }
//
//    executorService.submit{
//        Thread.sleep(1000)
//        println("map.containsKey(1) = ${map.containsKey(1)}")
//        Thread.sleep(3000)
//        println("map.containsKey(11) = ${map.containsKey(1)}")
//    }


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


//    println("gameCache.get(1) = ${gameCache.get(1)}")
//    println("gameCache.get(1) = ${map.get(1)}")
//    println("gameCache.get(2) = ${gameCache.get(2)}")
//    println("gameCache.get(3) = ${gameCache.get(3)}")
//    println("gameCache.get(4) = ${gameCache.get(4)}")

//    println("gameCache.size() = ${gameCache.size()}")
//
    executorService.shutdown()
    while (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
    }
    println("end")

}