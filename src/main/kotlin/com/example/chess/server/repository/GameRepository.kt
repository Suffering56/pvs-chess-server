package com.example.chess.server.repository

import com.example.chess.server.entity.Game
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
@Repository
interface GameRepository : JpaRepository<Game, Long>