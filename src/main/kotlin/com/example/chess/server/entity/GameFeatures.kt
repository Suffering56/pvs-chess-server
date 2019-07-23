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
    var sessionId: String?,

    @Column(nullable = true)
    var lastVisitDate: LocalDateTime?,

    @ColumnDefault("true")
    @Column(nullable = false)
    var longCastlingAvailable: Boolean,

    @ColumnDefault("true")
    @Column(nullable = false)
    var shortCastlingAvailable: Boolean,

    @Column(nullable = true)
    var pawnLongMoveColumnIndex: Int?,   //если пешка сделала длинный ход (на 2 клетки вперед) здесь храним индекс

    @ColumnDefault("false")
    @Column(nullable = false)
    var isUnderCheck: Boolean
)