import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { Property } from "../../core/observable/property/Property";
import { list_property, property } from "../../core/observable";
import { Disposable } from "../../core/observable/Disposable";
import { Disposer } from "../../core/observable/Disposer";
import { LogEntry, Logger, LogHandler, LogLevel, LogManager } from "../../core/Logger";

const logger = LogManager.get("quest_editor/stroes/LogStore");

export class LogStore implements Disposable {
    private readonly disposer = new Disposer();
    private readonly default_log_level = LogLevel.Info;

    private readonly log_buffer: LogEntry[] = [];
    private readonly logger_name_buffer: string[] = [];

    private readonly _level = property<LogLevel>(this.default_log_level);
    private readonly _log = list_property<LogEntry>();

    private readonly handler: LogHandler = (entry: LogEntry, logger_name: string): void => {
        this.buffer_log_entry(entry, logger_name);
    };

    readonly level: Property<LogLevel> = this._level;
    readonly log: ListProperty<LogEntry> = this._log.filtered(
        this.level.map(level => message => message.level >= level),
    );

    get_logger(name: string): Logger {
        const logger = LogManager.get(name);
        logger.handler = this.handler;
        return logger;
    }

    dispose(): void {
        this.disposer.dispose();
    }

    set_level(log_level: LogLevel): void {
        this._level.val = log_level;
    }

    private buffer_log_entry(entry: LogEntry, logger_name: string): void {
        this.log_buffer.push(entry);
        this.logger_name_buffer.push(logger_name);

        this.add_buffered_log_entries();
    }

    private adding_log_entries?: number;

    private add_buffered_log_entries(): void {
        if (this.adding_log_entries != undefined) return;

        this.adding_log_entries = requestAnimationFrame(() => {
            const DROP_THRESHOLD = 500;
            const DROP_THRESHOLD_HALF = DROP_THRESHOLD / 2;
            const BATCH_SIZE = 200;

            // Drop log entries if there are too many.
            if (this.log_buffer.length > DROP_THRESHOLD) {
                const drop_len = this.log_buffer.length - DROP_THRESHOLD;

                this.log_buffer.splice(DROP_THRESHOLD_HALF, drop_len, {
                    time: new Date(),
                    message: `...dropped ${drop_len} messages...`,
                    level: LogLevel.Warn,
                    logger,
                });
                this.logger_name_buffer.splice(
                    DROP_THRESHOLD_HALF,
                    drop_len,
                    "quest_editor/stores/LogStore",
                );
            }

            const len = Math.min(BATCH_SIZE, this.log_buffer.length);

            const buffered_entries = this.log_buffer.splice(0, len);
            const buffered_logger_names = this.logger_name_buffer.splice(0, len);

            this._log.push(...buffered_entries);

            for (let i = 0; i < len; i++) {
                const entry = buffered_entries[i];
                const logger_name = buffered_logger_names[i];
                LogManager.default_handler(entry, logger_name);
            }

            // Occasionally clean up old log messages if there are too many.
            if (this._log.length.val > 2000) {
                this._log.splice(0, 1000);
            }

            this.adding_log_entries = undefined;

            if (this.log_buffer.length) {
                this.add_buffered_log_entries();
            }
        });
    }
}

export const log_store = new LogStore();
