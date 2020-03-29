package com.example.chess.server.logic.misc

class Points {

    companion object {
        private val pointListsPoolOfOne: Array<List<Point>> = Array(Point.POINTS_POOL_SIZE) { compressedPoint ->
            listOf(Point.of(compressedPoint))
        }

        private val pointListsPoolOfTwo: Array<Array<List<Point>>> = Array(Point.POINTS_POOL_SIZE) { p1 ->
            Array(Point.POINTS_POOL_SIZE) { p2 ->
                listOf(Point.of(p1), Point.of(p2))
            }
        }

        private val pointListsPoolOfThree: Array<Array<Array<List<Point>>>> = Array(Point.POINTS_POOL_SIZE) { p1 ->
            Array(Point.POINTS_POOL_SIZE) { p2 ->
                Array(Point.POINTS_POOL_SIZE) { p3 ->
                    listOf(Point.of(p1), Point.of(p2), Point.of(p3))
                }
            }
        }

        fun empty(): List<Point> = Point.EMPTY_POINTS_LIST

        fun of(p1: Point): List<Point> = pointListsPoolOfOne[p1.compress()]

        fun of(p1: Point, p2: Point): List<Point> = pointListsPoolOfTwo[p1.compress()][p2.compress()]

        fun of(p1: Point, p2: Point, p3: Point): List<Point> = pointListsPoolOfThree[p1.compress()][p2.compress()][p3.compress()]
    }
}

fun List<Point>.with(point: Point): List<Point> {
    when (this.size) {
        0 -> {
            return Points.of(point)
        }
        1 -> {
            return Points.of(this[0], point)
        }
        2 -> {
            return Points.of(this[0], this[1], point)
        }
        3 -> {
            return mutableListOf(this[0], this[1], this[2], point)
        }
        else -> {
            check(this is MutableList) { "input list must be mutable!" }
            this.add(point)
            return this
        }
    }
}

