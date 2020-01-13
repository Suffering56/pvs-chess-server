package com.example.chess.server.service.impl

import com.example.chess.server.logic.IPoint
import com.example.chess.server.logic.misc.Cell
import com.example.chess.server.logic.misc.Point
import com.example.chess.shared.enums.Piece
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

/**
 * @author v.peschaniy
 *      Date: 13.01.2020
 */
@Component
class CellProvider {

    //TODO: cache
    @Value("\${cache_spec.equipments:expireAfterAccess=1h,maximumSize=300000,recordStats}")
    private lateinit var equipmentCacheSpec: String
    private lateinit var equipmentCache: LoadingCache<Cell, Cell>

    @PostConstruct
    private fun init() {
        equipmentCache = CacheBuilder.from(equipmentCacheSpec).build<Cell, Cell>(
            CacheLoader.from<Cell, Cell> { it }
        )
        //                { key -> if (key != null) createEquipment(key!!.getMessage()) else null })
    }

//    return equipmentCache.getUnchecked(ProtoMessageKey.createFor(equipmentProto));


    fun getOrCreate(row: Int, col: Int, piece: Piece) = Cell(row, col, piece)

    fun getOrCreate(point: IPoint, piece: Piece) = Cell(point.row, point.col, piece)

    fun getOrCreate(compressedPoint: Int, piece: Piece) = getOrCreate(Point.of(compressedPoint), piece)
}