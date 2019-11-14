package com.example.chess.server.entity

import com.example.chess.shared.dto.GameDTO
import com.example.chess.shared.enums.GameMode
import com.example.chess.shared.enums.Side
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.GenericGenerator
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
    val id: Long? = null,

    @ColumnDefault("0")
    @Column(nullable = false)
    var position: Int = 0,

    @ColumnDefault("'UNSELECTED'")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var mode: GameMode = GameMode.UNSELECTED,

    @OneToMany(mappedBy = "game", fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    @MapKey(name = "side")
    val featuresMap: Map<Side, GameFeatures> = emptyMap()
) {

    fun getSideFeatures(side: Side) = featuresMap.getValue(side)

    fun isUserRegistered(userId: String): Boolean {
        return getUserSide(userId).isPresent
    }

    fun registerUser(side: Side, userId: String) {
        getSideFeatures(side).userId = userId
    }

    fun getUserSide(userId: String): Optional<Side> {
        return featuresMap.values.stream()
            .filter { it.userId == userId }
            .map { it.side }
            .findAny()
    }

    private fun getFreeSideSlots(): List<Side> {
        return featuresMap.values.stream()
            .filter { it.userId == null }
            .map { it.side }
            .toList()
    }

    /**
     * Возвращает базовую информацию об игре:
     * - id игры
     * - position последнего хода
     * - на чьей стороне (side) сражается игрок с userId
     * - если сторона данным игроком еще не выбиралась (side == null), то вернет список незанятых сторон(freeSideSlots),
     *                                  которые игрок может занять, если он хочет присоединиться к игре.
     *                                  Если список пуст - значит все занято и user может принять участие в игре лишь как зритель
     * - если userId не указан, значит вернет
     */
    fun toDTO(userId: String?): GameDTO {
        if (userId == null) {
            return GameDTO(
                id!!,
                position,
                mode,
                null,
                getFreeSideSlots()
            )
        }

        val userSide = getUserSide(userId)
        val side: Side?
        val freeSideSlots: List<Side>

        if (!userSide.isPresent) {
            side = null
            freeSideSlots = getFreeSideSlots()
        } else {
            side = userSide.get()
            freeSideSlots = emptyList()
        }

        return GameDTO(id!!, position, mode, side, freeSideSlots)
    }

//    fun setLastVisitDate(side: Side, lastVisitDate: LocalDateTime) {
//        getSideFeatures(side).lastVisitDate = lastVisitDate
//    }
//
//    private fun disableShortCasting(side: Side) {
//        getSideFeatures(side).shortCastlingAvailable = false
//    }
//
//    private fun disableLongCasting(side: Side) {
//        getSideFeatures(side).longCastlingAvailable = false
//    }
//
//    fun disableCasting(side: Side) {
//        disableLongCasting(side)
//        disableShortCasting(side)
//    }
//
//    fun disableCasting(side: Side, rookColumnIndex: Int) {
//        when (rookColumnIndex) {
//            ROOK_SHORT_COLUMN_INDEX -> disableShortCasting(side)
//            ROOK_LONG_COLUMN_INDEX -> disableLongCasting(side)
//        }
//    }
//
//    fun isShortCastlingAvailable(side: Side) = getSideFeatures(side).shortCastlingAvailable
//
//    fun isLongCastlingAvailable(side: Side) = getSideFeatures(side).longCastlingAvailable
//
//    fun getPawnLongMoveColumnIndex(side: Side) = getSideFeatures(side).pawnLongMoveColumnIndex
//
//    fun setPawnLongMoveColumnIndex(side: Side, columnIndex: Int) {
//        getSideFeatures(side).pawnLongMoveColumnIndex = columnIndex
//    }
//
//    fun getUnderCheckSide(): Side? {
//        return featuresMap.values
//            .stream()
//            .filter { it.isUnderCheck }
//            .findAny()
//            .map { it.side }
//            .orElse(null)
//    }
//
//    fun setUnderCheckSide(side: Side?) {
//        featuresMap.values.forEach {
//            it.isUnderCheck = it.side == side
//        }
//    }
//
//    /**
//     * @return side, which has next move (not paused)
//     */
//    fun getNextTurnSideByPosition(): Side {
//        return if (position % 2 == 0)
//            Side.WHITE
//        else
//            Side.BLACK
//    }
}