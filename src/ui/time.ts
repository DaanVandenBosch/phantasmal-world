/**
 * @param hours can be fractional.
 * @returns a string of the shape ##:##.
 */
export function hours_to_string(hours: number): string {
    const h = Math.floor(hours);
    const m = Math.round(60 * (hours - h));
    return `${h.toString().padStart(2, "0")}:${m.toString().padStart(2, "0")}`;
}
