package com.example.chess.server.entity

import com.example.chess.shared.enums.PieceType
import org.hibernate.annotations.GenericGenerator
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

/**
 * @author v.peschaniy
 *      Date: 16.07.2019
 */
@Entity
data class History(

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

    @Column(nullable = true)
    val pieceFromPawn: PieceType?,

    @Column
    val description: String?
) {


//    val pointFrom: PointDTO
//        @Transient
//        get() = PointDTO.valueOf(rowIndexFrom, columnIndexFrom)
//
//    val pointTo: PointDTO
//        @Transient
//        get() = PointDTO.valueOf(rowIndexTo, columnIndexTo)
//
//    val formattedPosition: String
//        @Transient
//        get() = String.format("%02d", position)
//
//    @Transient
//    fun toExtendedMove(matrix: CellsMatrix): ExtendedMove {
//        val from = matrix.getCell(pointFrom)
//        val to = matrix.getCell(pointTo)
//        return ExtendedMove(from, to)
//    }
//
//    @Transient
//    fun toReadableString(): String {
//        return pointFrom + "-" + pointTo
//    }
}
