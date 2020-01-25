package com.example.chess.server.entity

import com.example.chess.server.logic.misc.Move
import com.example.chess.server.logic.misc.Point
import com.example.chess.shared.enums.Piece
import org.hibernate.annotations.GenericGenerator
import javax.persistence.*

/**
 * @author v.peschaniy
 *      Date: 16.07.2019
 */
@Entity
@Table(name = "history")
data class HistoryItem(

    @Id
    @GenericGenerator(
        name = "history_id_seq",
        strategy = "sequence-identity",
        parameters = [org.hibernate.annotations.Parameter(name = "sequence", value = "history_id_seq")]
    )
    @GeneratedValue(generator = "history_id_seq")
    val id: Long?,

    @Column(nullable = false)
    val gameId: Long,

    @Column(nullable = false)
    val position: Int,

    @Column(nullable = false)
    val rowIndexFrom: Int,

    @Column(nullable = false)
    val columnIndexFrom: Int,

    @Column(nullable = false)
    val rowIndexTo: Int,

    @Column(nullable = false)
    val columnIndexTo: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    val pieceFromPawn: Piece?,

    @Column
    val description: String?
) {

    fun toMove(): Move {
        return Move.of(
            Point.of(rowIndexFrom, columnIndexFrom),
            Point.of(rowIndexTo, columnIndexTo),
            pieceFromPawn
        )
    }

//    val from: PointDTO
//        @Transient
//        get() = PointDTO.valueOf(rowIndexFrom, columnIndexFrom)
//
//    val to: PointDTO
//        @Transient
//        get() = PointDTO.valueOf(rowIndexTo, columnIndexTo)
//
//    val formattedPosition: String
//        @Transient
//        get() = String.format("%02d", position)
//
//    @Transient
//    fun toExtendedMove(matrix: CellsMatrix): ExtendedMove {
//        val from = matrix.getCell(from)
//        val to = matrix.getCell(to)
//        return ExtendedMove(from, to)
//    }
//
//    @Transient
//    fun toReadableString(): String {
//        return from + "-" + to
//    }
}
