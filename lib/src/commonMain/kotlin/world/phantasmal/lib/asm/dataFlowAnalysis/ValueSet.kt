package world.phantasmal.lib.asm.dataFlowAnalysis

import kotlin.math.max
import kotlin.math.min

/**
 * Represents a sorted set of integers.
 */
class ValueSet private constructor(private val intervals: MutableList<Interval>) : Iterable<Int> {
    val size: Long
        get() = intervals.fold(0L) { acc, i -> acc + i.end - i.start + 1L }

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
     * Scalar addition.
     */
    operator fun plusAssign(scalar: Int) {
        if (scalar >= 0) {
            var i = 0
            var addI = 0

            while (i < intervals.size) {
                val int = intervals[i]
                val oldStart = int.start
                val oldEnd = int.end
                int.start += scalar
                int.end += scalar

                if (int.start < oldStart) {
                    // Integer overflow of both start and end.
                    intervals.removeAt(i)
                    intervals.add(addI++, int)
                } else if (int.end < oldEnd) {
                    // Integer overflow of end.
                    val newEnd = int.end
                    int.end = Int.MAX_VALUE

                    if (newEnd + 1 == intervals.first().start) {
                        intervals.first().start = Int.MIN_VALUE
                    } else {
                        intervals.add(0, Interval(Int.MIN_VALUE, newEnd))
                        addI++
                        // Increment i twice because we left this interval and inserted a new one.
                        i++
                    }
                }

                i++
            }
        } else {
            var i = intervals.lastIndex
            var addI = 0

            while (i >= 0) {
                val int = intervals[i]
                val oldStart = int.start
                val oldEnd = int.end
                int.start += scalar
                int.end += scalar

                if (int.end > oldEnd) {
                    // Integer underflow of both start and end.
                    intervals.removeAt(i)
                    intervals.add(intervals.size - addI++, int)
                } else if (int.start > oldStart) {
                    // Integer underflow of start.
                    val newStart = int.start
                    int.start = Int.MIN_VALUE

                    if (newStart - 1 == intervals.last().end) {
                        intervals.last().end = Int.MAX_VALUE
                    } else {
                        intervals.add(Interval(newStart, Int.MAX_VALUE))
                        addI++
                    }
                }

                i--
            }
        }
    }

    /**
     * Scalar subtraction.
     */
    operator fun minusAssign(scalar: Int) {
        plusAssign(-scalar)
    }

    /**
     * Doesn't take into account integer overflow.
     */
    operator fun timesAssign(s: Int) {
        for (int in intervals) {
            int.start *= s
            int.end *= s
        }
    }

    /**
     * Integer division.
     */
    operator fun divAssign(s: Int) {
        for (int in intervals) {
            int.start /= s
            int.end /= s
        }
    }

    fun union(other: ValueSet): ValueSet {
        var i = 0

        outer@ for (b in other.intervals) {
            while (i < intervals.size) {
                val a = intervals[i]

                if (b.end < a.start - 1L) {
                    // b lies entirely before a, insert it right before a.
                    intervals.add(i, b.copy())
                    i++
                    continue@outer
                } else if (b.start <= a.end + 1L) {
                    // a and b overlap or form a continuous interval (e.g. [1, 2] and [3, 4]).
                    a.start = min(a.start, b.start)

                    // Merge all intervals that overlap with b.
                    val j = i + 1

                    while (j < intervals.size) {
                        if (b.end >= intervals[j].start - 1L) {
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

    companion object {
        /**
         * Returns an empty [ValueSet].
         */
        fun empty(): ValueSet = ValueSet(mutableListOf())

        /**
         * Returns a [ValueSet] containing all possible Int values.
         */
        fun all(): ValueSet = ofInterval(Int.MIN_VALUE, Int.MAX_VALUE)

        /**
         * Returns a [ValueSet] with a single initial [value].
         */
        fun of(value: Int): ValueSet = ValueSet(mutableListOf(Interval(value, value)))

        /**
         * Returns a [ValueSet] with all values between [start] and [end], inclusively.
         */
        fun ofInterval(start: Int, end: Int): ValueSet =
            ValueSet(mutableListOf(Interval(start, end)))
    }
}

/**
 * Closed interval [start, end].
 */
private data class Interval(var start: Int, var end: Int) {
    operator fun contains(value: Int): Boolean =
        value in start..end
}
