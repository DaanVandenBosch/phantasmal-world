import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { Property } from "../../core/observable/property/Property";
import { list_property, property } from "../../core/observable";
import { Disposable } from "../../core/observable/Disposable";
import { Disposer } from "../../core/observable/Disposer";
import Logger from "js-logger";
import { enum_values } from "../../core/enums";
import { DateTime } from "luxon";

export type QuestLogger = {
    readonly debug: (message: string) => void;
    readonly info: (message: string) => void;
    readonly warning: (message: string) => void;
    readonly error: (message: string) => void;
};

// Log level names in order of importance.
export enum LogLevel {
    Debug,
    Info,
    Warning,
    Error,
}

export const LogLevels = enum_values<LogLevel>(LogLevel);

export type LogMessage = {
    readonly time: DateTime;
    readonly message: string;
    readonly level: LogLevel;
};

export class LogStore implements Disposable {
    private readonly disposer = new Disposer();
    private readonly default_log_level = LogLevel.Info;
    private readonly _log_messages = list_property<LogMessage>();
    private readonly _log_level = property<LogLevel>(this.default_log_level);

    readonly log_messages: ListProperty<LogMessage>;
    readonly log_level: Property<LogLevel> = this._log_level;

    constructor() {
        this.log_messages = this._log_messages.filtered(
            this.log_level.map(level => message => message.level >= level),
        );
    }

    dispose(): void {
        this.disposer.dispose();
    }

    set_log_level(log_level: LogLevel): void {
        this._log_level.val = log_level;
    }

    private add_log_message(message: string, level: LogLevel, logger_name: string): void {
        this._log_messages.push({
            time: DateTime.local(),
            message,
            level,
        });

        switch (level) {
            case LogLevel.Debug:
                Logger.get(logger_name).debug(message);
                break;
            case LogLevel.Info:
                Logger.get(logger_name).info(message);
                break;
            case LogLevel.Warning:
                Logger.get(logger_name).warn(message);
                break;
            case LogLevel.Error:
                Logger.get(logger_name).error(message);
                break;
        }
    }

    /**
     * @param name - js-logger logger name
     */
    get_logger(name: string): QuestLogger {
        return {
            debug: (message: string): void => {
                this.add_log_message(message, LogLevel.Debug, name);
            },
            info: (message: string): void => {
                this.add_log_message(message, LogLevel.Info, name);
            },
            warning: (message: string): void => {
                this.add_log_message(message, LogLevel.Warning, name);
            },
            error: (message: string): void => {
                this.add_log_message(message, LogLevel.Error, name);
            },
        };
    }
}

export const log_store = new LogStore();
