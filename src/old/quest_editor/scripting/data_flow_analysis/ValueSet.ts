/**
 * Represents a sorted set of integers.
 */
export class ValueSet {
    /**
     * Closed intervals [start, end].
     */
    private intervals: { start: number; end: number }[] = [];

    size(): number {
        return this.intervals.reduce((acc, i) => acc + i.end - i.start + 1, 0);
    }

    get(i: number): number | undefined {
        for (const { start, end } of this.intervals) {
            const size = end - start + 1;

            if (i < size) {
                return start + i;
            } else {
                i -= size;
            }
        }

        return undefined;
    }

    min(): number | undefined {
        return this.intervals.length ? this.intervals[0].start : undefined;
    }

    max(): number | undefined {
        return this.intervals.length ? this.intervals[this.intervals.length - 1].end : undefined;
    }

    has(value: number): boolean {
        for (const int of this.intervals) {
            if (int.start <= value && value <= int.end) {
                return true;
            }
        }

        return false;
    }

    /**
     * Sets this ValueSet to the given integer.
     *
     * @param value integer value
     */
    set_value(value: number): ValueSet {
        this.intervals = [{ start: value, end: value }];
        return this;
    }

    /**
     * Sets this ValueSet to the values in the given interval.
     *
     * @param start lower bound, inclusive
     * @param end upper bound, inclusive
     */
    set_interval(start: number, end: number): ValueSet {
        if (end < start)
            throw new Error(
                `Interval upper bound should be greater than or equal to lower bound, got [${start}, ${end}].`,
            );

        this.intervals = [{ start, end }];
        return this;
    }

    /**
     * Doesn't take into account interger overflow.
     */
    scalar_add(s: number): ValueSet {
        for (const int of this.intervals) {
            int.start += s;
            int.end += s;
        }

        return this;
    }

    /**
     * Doesn't take into account interger overflow.
     */
    scalar_sub(s: number): ValueSet {
        return this.scalar_add(-s);
    }

    /**
     * Doesn't take into account interger overflow.
     */
    scalar_mul(s: number): ValueSet {
        for (const int of this.intervals) {
            int.start *= s;
            int.end *= s;
        }

        return this;
    }

    /**
     * Integer division.
     * Doesn't take into account interger overflow.
     */
    scalar_div(s: number): ValueSet {
        for (const int of this.intervals) {
            int.start = Math.floor(int.start / s);
            int.end = Math.floor(int.end / s);
        }

        return this;
    }

    union(other: ValueSet): ValueSet {
        let i = 0;

        outer: for (const b of other.intervals) {
            while (i < this.intervals.length) {
                const a = this.intervals[i];

                if (b.end < a.start - 1) {
                    this.intervals.splice(i, 0, b);
                    i++;
                    continue outer;
                } else if (b.start <= a.end + 1) {
                    a.start = Math.min(a.start, b.start);

                    let j = i;

                    while (j < this.intervals.length) {
                        if (b.end > this.intervals[j].start) {
                            a.end = this.intervals[j].end;
                            j++;
                        } else {
                            break;
                        }
                    }

                    this.intervals.splice(i + 1, j - i - 1);
                    a.end = Math.max(a.end, b.end);
                    i++;
                    continue outer;
                } else {
                    i++;
                }
            }

            this.intervals.push(b);
        }

        return this;
    }

    to_array(): number[] {
        let array: number[] = [];

        for (const { start, end } of this.intervals) {
            for (let i = start; i <= end; i++) {
                array.push(i);
            }
        }

        return array;
    }

    [Symbol.iterator](): Iterator<number> {
        const vs = this;
        let int_i = 0;
        let value = NaN;

        return {
            next(): IteratorResult<number> {
                let done = true;

                if (int_i < vs.intervals.length) {
                    if (isNaN(value)) {
                        value = vs.intervals[int_i].start;
                        done = false;
                    } else if (value > vs.intervals[int_i].end) {
                        int_i++;

                        if (int_i < vs.intervals.length) {
                            value = vs.intervals[int_i].start;
                            done = false;
                        }
                    } else {
                        done = false;
                    }
                }

                return { done, value: value++ };
            },
        };
    }
}
