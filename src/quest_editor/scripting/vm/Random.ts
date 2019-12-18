export class Random {
    private holdrand: number;

    // PSO seeds the C standard library PRNG with Win32's GetTickCount().
    constructor(seed: number = Math.floor(performance.now())) {
        this.holdrand = seed;
    }

    /**
     * Emulates the MSVCRT C standard library.
     */
    next(): number {
        const r = (this.holdrand * 0x343fd + 0x269ec3) >>> 0;
        this.holdrand = r;
        return (r >>> 0x10) & 0x7fff;
    }
}
