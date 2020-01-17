import { Clock } from "../../../src/core/Clock";

export class StubClock implements Clock {
    constructor(private readonly date: Date) {}

    now(): Date {
        return this.date;
    }
}
