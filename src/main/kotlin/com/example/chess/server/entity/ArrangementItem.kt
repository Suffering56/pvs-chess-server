package com.example.chess.server.entity

import com.example.chess.shared.enums.Piece
import org.hibernate.annotations.GenericGenerator
import javax.persistence.*

@Table(name = "constructor_arrangement")
class ArrangementItem(

    @Id
    @GenericGenerator(
        name = "constructor_arrangement_id_seq",
        strategy = "sequence-identity",
        parameters = [org.hibernate.annotations.Parameter(name = "sequence", value = "constructor_arrangement_id_seq")]
    )
    @GeneratedValue(generator = "constructor_arrangement_id_seq")
    val id: Long?,

    @Column(nullable = false)
    val gameId: Long,

    @Column(nullable = false)
    val row: Int,

    @Column(nullable = false)
    val col: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val piece: Piece
)
