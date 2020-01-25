package com.example.chess.server.entity

import com.example.chess.server.logic.IGameFeatures
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

    @ColumnDefault("true")
    @Column(nullable = false)
    override var longCastlingAvailable: Boolean,        //TODO можно объединить с shortCastlingAvailable и хранить все в битовых флагах

    @ColumnDefault("true")
    @Column(nullable = false)
    override var shortCastlingAvailable: Boolean,

    @Column(nullable = true)
    override var pawnLongMoveColumnIndex: Int?,   //если пешка сделала длинный ход (на 2 клетки вперед) здесь храним индекс

    @ColumnDefault("false")
    @Column(nullable = false)
    var isUnderCheck: Boolean

) : IGameFeatures {

    fun disableCastling() {
        longCastlingAvailable = false
        shortCastlingAvailable = false
    }


    override fun toString(): String {
        return "GameFeatures(id=$id, " +
                "gameId=${game.id}, " +         //берем только id во избежание StackOverflow
                "side=$side, " +
                "userId=$userId, " +
                "lastVisitDate=$lastVisitDate, " +
                "longCastlingAvailable=$longCastlingAvailable, " +
                "shortCastlingAvailable=$shortCastlingAvailable, " +
                "pawnLongMoveColumnIndex=$pawnLongMoveColumnIndex, " +
                "isUnderCheck=$isUnderCheck)"
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
        if (longCastlingAvailable != other.longCastlingAvailable) return false
        if (shortCastlingAvailable != other.shortCastlingAvailable) return false
        if (pawnLongMoveColumnIndex != other.pawnLongMoveColumnIndex) return false
        if (isUnderCheck != other.isUnderCheck) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + game.id.hashCode()
        result = 31 * result + side.hashCode()
        result = 31 * result + (userId?.hashCode() ?: 0)
        result = 31 * result + (lastVisitDate?.hashCode() ?: 0)
        result = 31 * result + longCastlingAvailable.hashCode()
        result = 31 * result + shortCastlingAvailable.hashCode()
        result = 31 * result + (pawnLongMoveColumnIndex ?: 0)
        result = 31 * result + isUnderCheck.hashCode()
        return result
    }

    fun withoutCastlingEtc(owner: Game): GameFeatures {
        return GameFeatures(
            id,
            owner,
            side,
            userId,
            lastVisitDate,
            longCastlingAvailable = false,
            shortCastlingAvailable = false,
            pawnLongMoveColumnIndex = null,
            isUnderCheck = false
        )
    }
}