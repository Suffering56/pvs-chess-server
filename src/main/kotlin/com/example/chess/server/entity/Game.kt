package com.example.chess.server.entity

import com.example.chess.server.logic.IGame
import com.example.chess.shared.dto.GameDTO
import com.example.chess.shared.enums.GameMode
import com.example.chess.shared.enums.Side
import com.google.common.collect.Maps
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.GenericGenerator
import javax.persistence.*
import kotlin.streams.toList

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
    override val id: Long? = null,

    @ColumnDefault("0")
    @Column(nullable = false)
    override var position: Int = 0,

    @ColumnDefault("'UNSELECTED'")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    override val mode: GameMode = GameMode.UNSELECTED,


    /**
     * 0 - стандартная игра, созданная на основе расстановки по умолчанию
     * 1 - игра, созданная на основе конструктора. первыми ходят - черные (Side.BLACK)
     * 2 - игра, созданная на основе конструктора. первыми ходят - белые (Side.WHITE)
     */
    @ColumnDefault("0")
    @Column(nullable = false)
    override val initialPosition: Int = 0,

    @OneToMany(mappedBy = "game", fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    @MapKey(name = "side")
    val featuresMap: Map<Side, GameFeatures> = emptyMap()
) : IGame {

    private fun getSideFeatures(side: Side) = featuresMap.getValue(side)

    override fun isUserRegistered(userId: String): Boolean {
        return getUserSide(userId) != null
    }

    override fun isSideEmpty(side: Side): Boolean {
        return getSideFeatures(side).userId == null
    }

    fun registerUser(userId: String, side: Side) {
        getSideFeatures(side).userId = userId
    }

    override fun getUserSide(userId: String): Side? {
        return featuresMap.values.stream()
            .filter { it.userId == userId }
            .map { it.side }
            .findAny()
            .orElseGet(null)
    }

    override fun setPawnLongMoveColumnIndex(side: Side, index: Int?) {
        getSideFeatures(side).pawnLongMoveColumnIndex = null
    }

    override fun setUnderCheck(side: Side, isUnderCheck: Boolean) {
        getSideFeatures(side).isUnderCheck = isUnderCheck
    }

    override fun disableShortCastling(side: Side) {
        getSideFeatures(side).shortCastlingAvailable = false
    }

    override fun disableLongCastling(side: Side) {
        getSideFeatures(side).longCastlingAvailable = false
    }

    override fun isShortCastlingAvailable(side: Side): Boolean {
        return getSideFeatures(side).shortCastlingAvailable
    }

    override fun isLongCastlingAvailable(side: Side): Boolean {
        return getSideFeatures(side).longCastlingAvailable
    }

    override fun getPawnLongColumnIndex(side: Side): Int? {
        return getSideFeatures(side).pawnLongMoveColumnIndex
    }

    override fun getAndCheckBotSide(): Side {
        check(mode == GameMode.AI) { "wrong game mode. expected: AI, actual: $mode" }

        val freeSides = featuresMap.values.stream()
            .filter { it.userId == null }
            .map { it.side }
            .toList()

        check(freeSides.size == 1) { "cannot get bot side. expected free slots count: 1, actual: ${freeSides.size}" }
        return freeSides[0]
    }

    /**
     * Возвращает базовую информацию об игре:
     * - id игры
     * - position последнего хода
     * - side: на чьей стороне (side) сражается игрок с userId
     * - freeSide: сторону свободного слота (если таковой имеется) или null
     */
    override fun toDTO(userId: String): GameDTO {
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Game

        if (id != other.id) return false
        if (position != other.position) return false
        if (mode != other.mode) return false
        if (initialPosition != other.initialPosition) return false
        if (!Maps.difference(featuresMap, other.featuresMap).areEqual()) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + position
        result = 31 * result + mode.hashCode()
        result = 31 * result + initialPosition
        result = 31 * result + featuresMap.hashCode()
        return result
    }
}