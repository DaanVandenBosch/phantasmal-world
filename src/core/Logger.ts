import { enum_values } from "./enums";
import { assert } from "./util";

// Log level names in order of importance.
export enum LogLevel {
    Trace,
    Debug,
    Info,
    Warn,
    Error,
}

export const LogLevels = enum_values<LogLevel>(LogLevel);

export function log_level_from_string(str: string): LogLevel {
    const level = (LogLevel as any)[str.slice(0, 1).toUpperCase() + str.slice(1).toLowerCase()];
    assert(level != undefined, () => `"${str}" is not a valid log level.`);
    return level;
}

export type LogEntry = {
    readonly time: Date;
    readonly message: string;
    readonly level: LogLevel;
    readonly logger: Logger;
    readonly cause?: any;
};

export type LogHandler = (entry: LogEntry, logger_name: string) => void;

function default_log_handler({ time, message, level, logger, cause }: LogEntry): void {
    const str = `${time_to_string(time)} [${LogLevel[level]}] ${logger.name} - ${message}`;

    /* eslint-disable no-console */
    let method: (...args: any[]) => void;

    switch (level) {
        case LogLevel.Trace:
            method = console.trace;
            break;
        case LogLevel.Debug:
            method = console.debug;
            break;
        case LogLevel.Info:
            method = console.info;
            break;
        case LogLevel.Warn:
            method = console.warn;
            break;
        case LogLevel.Error:
            method = console.error;
            break;
        default:
            method = console.log;
    }

    if (cause == undefined) {
        method.call(console, str);
    } else {
        method.call(console, str, cause);
    }
    /* eslint-enable no-console */
}

export function time_to_string(time: Date): string {
    const hours = time_part_to_string(time.getHours(), 2);
    const minutes = time_part_to_string(time.getMinutes(), 2);
    const seconds = time_part_to_string(time.getSeconds(), 2);
    const millis = time_part_to_string(time.getMilliseconds(), 3);
    return `${hours}:${minutes}:${seconds}.${millis}`;
}

function time_part_to_string(value: number, n: number): string {
    return value.toString().padStart(n, "0");
}

export class Logger {
    private _level?: LogLevel;

    get level(): LogLevel {
        return this._level ?? LogManager.default_level;
    }

    set level(level: LogLevel) {
        this._level = level;
    }

    private _handler?: LogHandler;

    get handler(): LogHandler {
        return this._handler ?? LogManager.default_handler;
    }

    set handler(handler: LogHandler) {
        this._handler = handler;
    }

    constructor(readonly name: string) {}

    trace = (message: string, cause?: any): void => {
        this.handle(LogLevel.Trace, message, cause);
    };

    debug = (message: string, cause?: any): void => {
        this.handle(LogLevel.Debug, message, cause);
    };

    info = (message: string, cause?: any): void => {
        this.handle(LogLevel.Info, message, cause);
    };

    warn = (message: string, cause?: any): void => {
        this.handle(LogLevel.Warn, message, cause);
    };

    error = (message: string, cause?: any): void => {
        this.handle(LogLevel.Error, message, cause);
    };

    private handle(level: LogLevel, message: string, cause?: any): void {
        if (level >= this.level) {
            this.handler({ time: new Date(), message, level, logger: this, cause }, this.name);
        }
    }
}

export class LogManager {
    private static readonly loggers = new Map<string, Logger>();

    static default_level: LogLevel = log_level_from_string(process.env["LOG_LEVEL"] ?? "Info");
    static default_handler: LogHandler = default_log_handler;

    static get(name: string): Logger {
        let logger = this.loggers.get(name);

        if (!logger) {
            logger = new Logger(name);
            this.loggers.set(name, logger);
        }

        return logger;
    }

    static with_default_handler<T>(handler: LogHandler, f: () => T): T {
        const orig_handler = this.default_handler;
        let is_promise = false;

        try {
            this.default_handler = handler;
            const r = f();

            if (r instanceof Promise) {
                is_promise = true;
                return (r.finally(() => (this.default_handler = orig_handler)) as unknown) as T;
            } else {
                return r;
            }
        } finally {
            if (!is_promise) {
                this.default_handler = orig_handler;
            }
        }
    }
}
