/**
 * Represents a set of integers.
 */
export class ValueSet {
    /**
     * Open intervals [start, end[.
     */
    private intervals: { start: number; end: number }[] = [];

    size(): number {
        return this.intervals.reduce((acc, i) => acc + i.end - i.start, 0);
    }

    /**
     * Sets this ValueSet to the given integer.
     *
     * @param value integer value
     */
    set_value(value: number): ValueSet {
        this.intervals = [{ start: value, end: value + 1 }];
        return this;
    }

    /**
     * Sets this ValueSet to the values in the given interval.
     *
     * @param start lower bound, inclusive
     * @param end upper bound, exclusive
     */
    set_interval(start: number, end: number): ValueSet {
        if (end < start)
            throw new Error(
                `Interval upper bound should be greater than lower bound, got [${start}, ${end}[.`
            );

        if (end !== start) {
            this.intervals = [{ start, end }];
        }

        return this;
    }

    union(other: ValueSet): ValueSet {
        let i = 0;

        outer: for (const b of other.intervals) {
            while (i < this.intervals.length) {
                const a = this.intervals[i];

                if (b.end < a.start) {
                    this.intervals.splice(i, 0, b);
                    i++;
                    continue outer;
                } else if (b.start <= a.end) {
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
                    } else if (value >= vs.intervals[int_i].end) {
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
