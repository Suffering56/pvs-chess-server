package com.example.chess.entity

import com.example.chess.enums.GameMode
import com.example.chess.shared.enums.Side
import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.common.base.Preconditions.checkState
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.GenericGenerator
import javax.persistence.*

@Entity
data class Game(
    @Id
    @GenericGenerator(
        name = "game_id_seq",
        strategy = "sequence-identity",
        parameters = [org.hibernate.annotations.Parameter(name = "sequence", value = "game_id_seq")]
    )
    @GeneratedValue(generator = "game_id_seq")
    var id: Long? = null,

    @ColumnDefault("0")
    @Column(nullable = false)
    var position: Int,

    @ColumnDefault("UNSELECTED")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var mode: GameMode,

    @OneToMany(mappedBy = "game", fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    @MapKey(name = "side")
    val featuresMap: Map<Side, GameFeatures>
) {
    companion object {
        const val ROOK_SHORT_COLUMN_INDEX = 0
        const val ROOK_LONG_COLUMN_INDEX = 7
    }

    @Transient
    private fun getSideFeatures(side: Side) = featuresMap.getValue(side)

    @Transient
    private fun disableShortCasting(side: Side) {
        getSideFeatures(side).shortCastlingAvailable = false
    }

    @Transient
    private fun disableLongCasting(side: Side) {
        getSideFeatures(side).longCastlingAvailable = false
    }

    @Transient
    fun disableCasting(side: Side) {
        disableLongCasting(side)
        disableShortCasting(side)
    }

    @Transient
    fun disableCasting(side: Side, rookColumnIndex: Int) {
        when (rookColumnIndex) {
            ROOK_SHORT_COLUMN_INDEX -> disableShortCasting(side)
            ROOK_LONG_COLUMN_INDEX -> disableLongCasting(side)
        }
    }

    @Transient
    fun isShortCastlingAvailable(side: Side) = getSideFeatures(side).shortCastlingAvailable

    @Transient
    fun isLongCastlingAvailable(side: Side) = getSideFeatures(side).longCastlingAvailable

    @Transient
    fun getPawnLongMoveColumnIndex(side: Side) = getSideFeatures(side).pawnLongMoveColumnIndex

    @Transient
    fun setPawnLongMoveColumnIndex(side: Side, columnIndex: Int) {
        getSideFeatures(side).pawnLongMoveColumnIndex = columnIndex
    }

    @Transient
    fun getUnderCheckSide(): Side? {
        return featuresMap.values
            .stream()
            .filter { it.isUnderCheck }
            .findAny()
            .map { it.side }
            .orElse(null)
    }

    @Transient
    fun setUnderCheckSide(side: Side?) {
        featuresMap.values.forEach {
            it.isUnderCheck = it.side == side
        }
    }

    /**
     * Only works in AI mode
     */
    @Transient
    @JsonIgnore
    fun getPlayerSide(): Side? {
        checkState(mode == GameMode.AI, "Game mode is not AI!")

        if (getSideFeatures(Side.WHITE).sessionId != null && getSideFeatures(Side.BLACK).sessionId == null) {
            return Side.WHITE
        }
        if (getSideFeatures(Side.BLACK).sessionId != null && getSideFeatures(Side.WHITE).sessionId == null) {
            return Side.BLACK
        }
        return null
    }

    /**
     * @return side, which has next move (not paused)
     */
    @Transient
    @JsonIgnore
    fun getActiveSide(): Side {
        return if (position % 2 == 0)
            Side.WHITE
        else
            Side.BLACK
    }

}