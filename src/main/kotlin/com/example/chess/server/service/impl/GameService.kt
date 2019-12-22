package com.example.chess.server.service.impl

import com.example.chess.server.entity.Game
import com.example.chess.server.entity.provider.EntityProvider
import com.example.chess.server.logic.IChessboard
import com.example.chess.server.logic.IGame
import com.example.chess.server.logic.IMutableChessboard
import com.example.chess.server.logic.misc.isLongPawnMove
import com.example.chess.server.logic.misc.isPawnTransformation
import com.example.chess.server.logic.misc.toPrettyString
import com.example.chess.server.repository.GameRepository
import com.example.chess.server.repository.HistoryRepository
import com.example.chess.server.service.IGameService
import com.example.chess.server.service.IMovesProvider
import com.example.chess.shared.Constants.ROOK_LONG_COLUMN_INDEX
import com.example.chess.shared.Constants.ROOK_SHORT_COLUMN_INDEX
import com.example.chess.shared.api.IMove
import com.example.chess.shared.api.IPoint
import com.example.chess.shared.dto.ChangesDTO
import com.example.chess.shared.enums.GameMode
import com.example.chess.shared.enums.PieceType
import com.example.chess.shared.enums.Side
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
    private val movesProvider: IMovesProvider
) : IGameService {

    @Transactional
    override fun createNewGame(userId: String, mode: GameMode, side: Side, isConstructor: Boolean): Game {
        return gameRepository.save(entityProvider.createNewGame(userId, mode, side, isConstructor))
    }

    @Transactional
    override fun saveGame(game: Game): Game {
        return gameRepository.save(game)
    }

    override fun findAndCheckGame(gameId: Long): Game {
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

        val enemySide = piece.side.reverse()
        val sideFeatures = game.getSideFeatures(piece.side)
        val enemySideFeatures = game.getSideFeatures(enemySide)

        val additionalMove = chessboard.applyMove(move)

        game.position = chessboard.position
        // очищаем стейт взятия на проходе, т.к. оно допустимо только в течении 1го раунда
        sideFeatures.pawnLongMoveColumnIndex = null

        val underCheck = movesProvider.isUnderCheck(enemySide, chessboard)
        sideFeatures.isUnderCheck =
            false               // мы не можем сделать такой ход, после которого окажемся под шахом
        enemySideFeatures.isUnderCheck = underCheck     // но наш ход, может причинить шах противнику

        when (piece.type) {
            PieceType.KING -> {
                sideFeatures.disableCastling()
            }
            PieceType.ROOK -> {
                if (sideFeatures.longCastlingAvailable && move.from.col == ROOK_LONG_COLUMN_INDEX) {
                    sideFeatures.longCastlingAvailable = false
                } else if (sideFeatures.shortCastlingAvailable && move.from.col == ROOK_SHORT_COLUMN_INDEX) {
                    sideFeatures.shortCastlingAvailable = false
                }
            }
            PieceType.PAWN -> {
                if (move.isLongPawnMove(piece)) {
                    sideFeatures.pawnLongMoveColumnIndex = move.from.col
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

        return saveGame(game)
    }
}