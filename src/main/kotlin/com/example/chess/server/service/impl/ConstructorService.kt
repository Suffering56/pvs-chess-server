package com.example.chess.server.service.impl

import com.example.chess.server.entity.provider.EntityProvider
import com.example.chess.server.logic.IUnmodifiableGame
import com.example.chess.server.logic.misc.Point
import com.example.chess.server.repository.ArrangementRepository
import com.example.chess.server.service.IConstructorService
import com.example.chess.shared.dto.ChessboardDTO
import com.example.chess.shared.dto.ConstructorGameDTO
import com.example.chess.shared.enums.Side
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import kotlin.streams.toList

@Service
class ConstructorService @Autowired constructor(
    private val entityProvider: EntityProvider,
    private val arrangementRepository: ArrangementRepository,
    private val chessboardProvider: ChessboardProvider,
    private val movesProvider: MovesProvider
) : IConstructorService {

    override fun initArrangement(game: IUnmodifiableGame, side: Side, clientChessboard: ChessboardDTO): ConstructorGameDTO {
        validate(clientChessboard)

        val arrangement = Arrays.stream(clientChessboard.matrix)
            .flatMap { Arrays.stream(it) }
            .filter { it.piece != null }
            .map {
                entityProvider.createConstructorArrangementItem(
                    game.id!!,
                    Point.of(it.pointDTO),
                    it.piece!!
                )
            }
            .toList()

        val initialArrangement = arrangementRepository.saveAll(arrangement)

        val serverChessboard = chessboardProvider.createChessboardForGameWithArrangement(game, game.position, initialArrangement)
        val underCheck = movesProvider.isUnderCheck(side.reverse(), serverChessboard)

        return ConstructorGameDTO(
            game.id!!,
            game.position,
            serverChessboard.toDTO().matrix,
            if (!underCheck) null else serverChessboard.getKingPoint(side.reverse()).toDTO()
        )
    }

    private fun validate(chessboard: ChessboardDTO) {
        //TODO: NYI
    }

}