import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { Property } from "../../core/observable/property/Property";
import { list_property, property } from "../../core/observable";
import { Disposable } from "../../core/observable/Disposable";
import { Disposer } from "../../core/observable/Disposer";
import Logger from "js-logger";
import { enum_values } from "../../core/enums";

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
    readonly time: Date;
    readonly message: string;
    readonly level: LogLevel;
};

export class LogStore implements Disposable {
    private readonly disposer = new Disposer();
    private readonly default_log_level = LogLevel.Info;

    private readonly message_buffer: LogMessage[] = [];
    private readonly logger_name_buffer: string[] = [];

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

    /**
     * @param name - js-logger logger name
     */
    get_logger(name: string): QuestLogger {
        return {
            debug: (message: string): void => {
                this.buffer_log_message(message, LogLevel.Debug, name);
            },
            info: (message: string): void => {
                this.buffer_log_message(message, LogLevel.Info, name);
            },
            warning: (message: string): void => {
                this.buffer_log_message(message, LogLevel.Warning, name);
            },
            error: (message: string): void => {
                this.buffer_log_message(message, LogLevel.Error, name);
            },
        };
    }

    private buffer_log_message(message: string, level: LogLevel, logger_name: string): void {
        this.message_buffer.push({
            time: new Date(),
            message,
            level,
        });
        this.logger_name_buffer.push(logger_name);

        this.add_buffered_log_messages();
    }

    private adding_log_messages?: number;

    private add_buffered_log_messages(): void {
        if (this.adding_log_messages != undefined) return;

        this.adding_log_messages = requestAnimationFrame(() => {
            const DROP_THRESHOLD = 500;
            const DROP_THRESHOLD_HALF = DROP_THRESHOLD / 2;
            const BATCH_SIZE = 200;

            // Drop messages if there are too many.
            if (this.message_buffer.length > DROP_THRESHOLD) {
                const drop_len = this.message_buffer.length - DROP_THRESHOLD;

                this.message_buffer.splice(DROP_THRESHOLD_HALF, drop_len, {
                    time: new Date(),
                    message: `...dropped ${drop_len} messages...`,
                    level: LogLevel.Warning,
                });
                this.logger_name_buffer.splice(
                    DROP_THRESHOLD_HALF,
                    drop_len,
                    "quest_editor/stores/LogStore",
                );
            }

            const len = Math.min(BATCH_SIZE, this.message_buffer.length);

            const buffered_messages = this.message_buffer.splice(0, len);
            const buffered_logger_names = this.logger_name_buffer.splice(0, len);

            this._log_messages.push(...buffered_messages);

            for (let i = 0; i < len; i++) {
                const { level, message } = buffered_messages[i];
                const logger_name = buffered_logger_names[i];

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

            // Occasionally clean up old log messages if there are too many.
            if (this._log_messages.length.val > 2000) {
                this._log_messages.splice(0, 1000);
            }

            this.adding_log_messages = undefined;

            if (this.message_buffer.length) {
                this.add_buffered_log_messages();
            }
        });
    }
}

export const log_store = new LogStore();
