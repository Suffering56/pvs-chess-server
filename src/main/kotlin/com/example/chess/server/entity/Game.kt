package com.example.chess.server.entity

import com.example.chess.server.logic.IGame
import com.example.chess.shared.dto.GameDTO
import com.example.chess.shared.enums.GameMode
import com.example.chess.shared.enums.Side
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.GenericGenerator
import javax.persistence.*

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
@Entity
data class Game(
    @Id
    @GenericGenerator(
        name = "game_id_seq",
        strategy = "sequence-identity",
        parameters = [org.hibernate.annotations.Parameter(name = "sequence", value = "game_id_seq")]
    )
    @GeneratedValue(generator = "game_id_seq")
    val id: Long? = null,

    @ColumnDefault("0")
    @Column(nullable = false)
    var position: Int = 0,

    @ColumnDefault("'UNSELECTED'")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val mode: GameMode = GameMode.UNSELECTED,


    /**
     * 0 - стандартная игра, созданная на основе расстановки по умолчанию
     * 1 - игра, созданная на основе конструктора. первыми ходят - черные (Side.BLACK)
     * 2 - игра, созданная на основе конструктора. первыми ходят - белые (Side.WHITE)
     */
    @ColumnDefault("0")
    @Column(nullable = false)
    val initialPosition: Int = 0,

    @OneToMany(mappedBy = "game", fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    @MapKey(name = "side")
    val featuresMap: Map<Side, GameFeatures> = emptyMap()
) : IGame {

    override fun getSideFeatures(side: Side) = featuresMap.getValue(side)

    fun isUserRegistered(userId: String): Boolean {
        return getUserSide(userId) != null
    }

    fun registerUser(side: Side, userId: String) {
        getSideFeatures(side).userId = userId
    }

    fun getUserSide(userId: String): Side? {
        return featuresMap.values.stream()
            .filter { it.userId == userId }
            .map { it.side }
            .findAny()
            .orElseGet(null)
    }

    /**
     * Возвращает базовую информацию об игре:
     * - id игры
     * - position последнего хода
     * - side: на чьей стороне (side) сражается игрок с userId
     * - freeSide: сторону свободного слота (если таковой имеется) или null
     */
    fun toDTO(userId: String): GameDTO {
        val userSide = requireNotNull(getUserSide(userId)) {
            "user with id: $userId was not register in current game(id=$id)"
        }

        val opponentSide = userSide.reverse()
        val hasFreeSlot = getSideFeatures(opponentSide).userId == null

        return GameDTO(
            id!!,
            position,
            mode,
            userSide,
            if (hasFreeSlot) opponentSide else null
        )
    }
}