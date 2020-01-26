package com.example.chess.server.entity

import com.example.chess.shared.enums.Side
import com.fasterxml.jackson.annotation.JsonIgnore
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.GenericGenerator
import java.time.LocalDateTime
import javax.persistence.*

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
@Entity
data class GameFeatures(

    @Id
    @GenericGenerator(
        name = "game_features_id_seq",
        strategy = "sequence-identity",
        parameters = [org.hibernate.annotations.Parameter(name = "sequence", value = "game_features_id_seq")]
    )
    @GeneratedValue(generator = "game_features_id_seq")
    val id: Long?,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "game_id", nullable = false)
    val game: Game,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val side: Side,

    @Column(nullable = true)
    var userId: String?,

    @Column(nullable = true)
    var lastVisitDate: LocalDateTime?,

    @ColumnDefault("$ALL_CASTLING_ENABLED")
    @Column(nullable = false)
    var castlingState: Int,

    @Column(nullable = true)
    var pawnLongMoveColumnIndex: Int?   //если пешка сделала длинный ход (на 2 клетки вперед) здесь храним индекс

) {
    companion object {
        const val ALL_CASTLING_ENABLED = 0b11
        const val SHORT_CASTLING_ENABLED = 0b01
        const val LONG_CASTLING_ENABLED = 0b10
        const val ALL_CASTLING_DISABLED = 0b00
    }

    fun isShortCastlingAvailable(): Boolean {
        return castlingState.and(SHORT_CASTLING_ENABLED) == SHORT_CASTLING_ENABLED
    }

    fun isLongCastlingAvailable(): Boolean {
        return castlingState.and(LONG_CASTLING_ENABLED) == LONG_CASTLING_ENABLED
    }

    fun enableShortCastling() {
        castlingState = castlingState.or(SHORT_CASTLING_ENABLED)
    }

    fun enableLongCastling() {
        castlingState = castlingState.or(LONG_CASTLING_ENABLED)
    }

    fun disableShortCastling() {
        castlingState = castlingState.and(LONG_CASTLING_ENABLED)
    }

    fun disableLongCastling() {
        castlingState = castlingState.and(SHORT_CASTLING_ENABLED)
    }

    fun disableCastling() {
        castlingState = ALL_CASTLING_DISABLED
    }

    fun withoutCastlingEtc(owner: Game): GameFeatures {
        return GameFeatures(
            id,
            owner,
            side,
            userId,
            lastVisitDate,
            castlingState = ALL_CASTLING_DISABLED,
            pawnLongMoveColumnIndex = null
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameFeatures

        if (id != other.id) return false
        if (game.id != other.game.id) return false
        if (side != other.side) return false
        if (userId != other.userId) return false
        if (lastVisitDate != other.lastVisitDate) return false
        if (castlingState != other.castlingState) return false
        if (pawnLongMoveColumnIndex != other.pawnLongMoveColumnIndex) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + game.id.hashCode()
        result = 31 * result + side.hashCode()
        result = 31 * result + (userId?.hashCode() ?: 0)
        result = 31 * result + (lastVisitDate?.hashCode() ?: 0)
        result = 31 * result + castlingState.hashCode()
        result = 31 * result + (pawnLongMoveColumnIndex ?: 0)
        return result
    }

    override fun toString(): String {
        return "GameFeatures(id=$id, " +
                "gameId=${game.id}, " +         //берем только id во избежание StackOverflow
                "side=$side, " +
                "userId=$userId, " +
                "lastVisitDate=$lastVisitDate, " +
                "castlingState=${Integer.toBinaryString(castlingState)}, " +
                "pawnLongMoveColumnIndex=$pawnLongMoveColumnIndex)"
    }
}