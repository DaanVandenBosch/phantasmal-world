export class Random {
    constructor(private readonly random_number: () => number = Math.random) {}
    /**
     * @param min - The minimum value, inclusive.
     * @param max - The maximum value, exclusive.
     * @returns A random integer between `min` and `max`.
     */
    integer(min: number, max: number): number {
        return min + Math.floor(this.random_number() * (max - min));
    }

    /**
     * @returns A random element from `array`.
     */
    sample_array<T>(array: readonly T[]): T {
        return array[this.integer(0, array.length)];
    }
}
