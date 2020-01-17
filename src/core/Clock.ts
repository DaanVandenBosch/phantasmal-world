export interface Clock {
    now(): Date;
}

export class DateClock implements Clock {
    now(): Date {
        return new Date();
    }
}
