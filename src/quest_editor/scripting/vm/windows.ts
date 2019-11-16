/**
 * @file Implementations of some parts of the Win32 API and the MSVCRT C standard library.
 */

let holdrand = 1;

export function srand(seed: number): void {
    holdrand = seed;
}

export function rand(): number {
    const r = (holdrand * 0x343fd + 0x269ec3) >>> 0;
    holdrand = r;
    return (r >>> 0x10) & 0x7fff;
}

export function GetTickCount(): number {
    // GetTickCount returns time elapsed since system start
    // performance.now returns time elapsed since document load
    // but both are monotonic so it's probably close enough?
    return Math.floor(performance.now());
}
