package com.example.chess.server.entity

import com.example.chess.shared.Constants.ROOK_LONG_COLUMN_INDEX
import com.example.chess.shared.Constants.ROOK_SHORT_COLUMN_INDEX
import com.example.chess.shared.dto.GameDTO
import com.example.chess.shared.enums.GameMode
import com.example.chess.shared.enums.Side
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.GenericGenerator
import java.time.LocalDateTime
import java.util.*
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
    val id: Long?,

    @ColumnDefault("0")
    @Column(nullable = false)
    var position: Int,

    @ColumnDefault("'UNSELECTED'")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var mode: GameMode,

    @OneToMany(mappedBy = "game", fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    @MapKey(name = "side")
    val featuresMap: Map<Side, GameFeatures>
) {
    //пустой конструктор нужен для того, чтобы Game можно было инжектить через @InjectGame
    constructor() : this(null, 0, GameMode.UNSELECTED, EnumMap(Side::class.java))

    fun getSideFeatures(side: Side) = featuresMap.getValue(side)

    fun setLastVisitDate(side: Side, lastVisitDate: LocalDateTime) {
        getSideFeatures(side).lastVisitDate = lastVisitDate
    }

    fun setUserId(side: Side, userId: String) {
        getSideFeatures(side).userId = userId
    }

    private fun disableShortCasting(side: Side) {
        getSideFeatures(side).shortCastlingAvailable = false
    }

    private fun disableLongCasting(side: Side) {
        getSideFeatures(side).longCastlingAvailable = false
    }

    fun disableCasting(side: Side) {
        disableLongCasting(side)
        disableShortCasting(side)
    }

    fun disableCasting(side: Side, rookColumnIndex: Int) {
        when (rookColumnIndex) {
            ROOK_SHORT_COLUMN_INDEX -> disableShortCasting(side)
            ROOK_LONG_COLUMN_INDEX -> disableLongCasting(side)
        }
    }

    fun isShortCastlingAvailable(side: Side) = getSideFeatures(side).shortCastlingAvailable

    fun isLongCastlingAvailable(side: Side) = getSideFeatures(side).longCastlingAvailable

    fun getPawnLongMoveColumnIndex(side: Side) = getSideFeatures(side).pawnLongMoveColumnIndex

    fun setPawnLongMoveColumnIndex(side: Side, columnIndex: Int) {
        getSideFeatures(side).pawnLongMoveColumnIndex = columnIndex
    }

    fun getUnderCheckSide(): Side? {
        return featuresMap.values
            .stream()
            .filter { it.isUnderCheck }
            .findAny()
            .map { it.side }
            .orElse(null)
    }

    fun setUnderCheckSide(side: Side?) {
        featuresMap.values.forEach {
            it.isUnderCheck = it.side == side
        }
    }

    /**
     * Only works in AI mode
     */
    fun getPlayerSide(): Side? {
        check(mode == GameMode.AI) { "Game mode is not AI!" }

        if (getSideFeatures(Side.WHITE).userId != null && getSideFeatures(Side.BLACK).userId == null) {
            return Side.WHITE
        }
        if (getSideFeatures(Side.BLACK).userId != null && getSideFeatures(Side.WHITE).userId == null) {
            return Side.BLACK
        }
        return null
    }

    /**
     * @return side, which has next move (not paused)
     */
    fun getNextTurnSideByPosition(): Side {
        return if (position % 2 == 0)
            Side.WHITE
        else
            Side.BLACK
    }

    fun toDTO(userId: String): GameDTO {
        val userSide = getUserSide(userId)
        val side: Side?
        val freeSideSlots: List<Side>

        if (userSide.isPresent) {
            side = userSide.get()
            freeSideSlots = emptyList()
        } else {
            side = null
            freeSideSlots = featuresMap.values.stream()
                .filter { it.userId == null }
                .map { it.side }
                .toList()
        }

        return GameDTO(id!!, position, mode, side, freeSideSlots)
    }

    fun toDTO() = GameDTO(id!!, position, mode, null, listOf(Side.WHITE, Side.BLACK))

    fun isUserRegistered(userId: String): Boolean {
        return getUserSide(userId).isPresent
    }

    private fun getUserSide(userId: String): Optional<Side> {
        return featuresMap.values.stream()
            .filter { it.userId == userId }
            .map { it.side }
            .findAny()
    }
}