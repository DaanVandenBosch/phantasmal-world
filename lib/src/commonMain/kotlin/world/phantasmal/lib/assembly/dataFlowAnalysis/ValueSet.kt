package world.phantasmal.lib.assembly.dataFlowAnalysis

import kotlin.math.max
import kotlin.math.min

/**
 * Represents a sorted set of integers.
 */
class ValueSet : Iterable<Int> {
    private val intervals: MutableList<Interval> = mutableListOf()

    val size: Int
        get() =
            intervals.fold(0) { acc, i -> acc + i.end - i.start + 1 }

    operator fun get(i: Int): Int? {
        var idx = i

        for ((start, end) in intervals) {
            val size = end - start + 1

            if (idx < size) {
                return start + idx
            } else {
                idx -= size
            }
        }

        return null
    }

    fun isEmpty(): Boolean =
        intervals.isEmpty()

    fun minOrNull(): Int? =
        intervals.firstOrNull()?.start

    fun maxOrNull(): Int? =
        intervals.lastOrNull()?.end

    operator fun contains(value: Int): Boolean {
        for (int in intervals) {
            if (value in int) {
                return true
            }
        }

        return false
    }

    /**
     * Sets this ValueSet to the given integer.
     */
    fun setValue(value: Int): ValueSet {
        intervals.clear()
        intervals.add(Interval(value, value))
        return this
    }

    /**
     * Sets this ValueSet to the values in the given interval.
     *
     * @param start lower bound, inclusive
     * @param end upper bound, inclusive
     */
    fun setInterval(start: Int, end: Int): ValueSet {
        require(end >= start) {
            "Interval upper bound should be greater than or equal to lower bound, got [${start}, ${end}]."
        }

        intervals.clear()
        intervals.add(Interval(start, end))
        return this
    }

    /**
     * Doesn't take into account integer overflow.
     */
    fun scalarAdd(s: Int): ValueSet {
        for (int in intervals) {
            int.start += s
            int.end += s
        }

        return this
    }

    /**
     * Doesn't take into account integer overflow.
     */
    fun scalarSub(s: Int): ValueSet {
        return scalarAdd(-s)
    }

    /**
     * Doesn't take into account integer overflow.
     */
    fun scalarMul(s: Int): ValueSet {
        for (int in intervals) {
            int.start *= s
            int.end *= s
        }

        return this
    }

    /**
     * Integer division.
     */
    fun scalarDiv(s: Int): ValueSet {
        for (int in intervals) {
            int.start = int.start / s
            int.end = int.end / s
        }

        return this
    }

    fun union(other: ValueSet): ValueSet {
        var i = 0

        outer@ for (b in other.intervals) {
            while (i < intervals.size) {
                val a = intervals[i]

                if (b.end < a.start - 1) {
                    // b lies entirely before a, insert it right before a.
                    intervals.add(i, b.copy())
                    i++
                    continue@outer
                } else if (b.start <= a.end + 1) {
                    // a and b overlap or form a continuous interval (e.g. [1, 2] and [3, 4]).
                    a.start = min(a.start, b.start)

                    // Merge all intervals that overlap with b.
                    val j = i + 1

                    while (j < intervals.size) {
                        if (b.end >= intervals[j].start - 1) {
                            a.end = intervals[j].end
                            intervals.removeAt(j)
                        } else {
                            break
                        }
                    }

                    a.end = max(a.end, b.end)
                    i++
                    continue@outer
                } else {
                    // b lies entirely after a, check next a.
                    i++
                }
            }

            // b lies after every a, add it to the end of our intervals.
            intervals.add(b.copy())
        }

        return this
    }

    override fun iterator(): Iterator<Int> =
        object : Iterator<Int> {
            private var intIdx = 0
            private var nextValue: Int? = minOrNull()

            override fun hasNext(): Boolean =
                nextValue != null

            override fun next(): Int {
                val v = nextValue ?: throw NoSuchElementException()

                nextValue =
                    if (v < intervals[intIdx].end) {
                        v + 1
                    } else {
                        intIdx++

                        if (intIdx < intervals.size) {
                            intervals[intIdx].start
                        } else {
                            null
                        }
                    }

                return v
            }
        }
}

/**
 * Closed interval [start, end].
 */
private data class Interval(var start: Int, var end: Int) {
    operator fun contains(value: Int): Boolean =
        value in start..end
}
