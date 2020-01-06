import { Logger } from "./Logger";
import { Severity } from "./Severity";

export type Result<T> = Success<T> | Failure;

export type Success<T> = {
    readonly success: true;
    readonly value: T;
    readonly problems: readonly Problem[];
};

export type Failure = {
    readonly success: false;
    readonly value?: never;
    readonly problems: readonly Problem[];
};

export type Problem = {
    readonly severity: Severity;
    /**
     * Readable message meant for users.
     */
    readonly ui_message: string;
};

export function success<T>(value: T, problems?: readonly Problem[]): Success<T> {
    return {
        success: true,
        value,
        problems: problems ?? [],
    };
}

export function failure(problems?: readonly Problem[]): Failure {
    return {
        success: false,
        problems: problems ?? [],
    };
}

/**
 * "Unwraps" the given result by either return its value if it's a success or throwing an error with
 * its problems as message if it was a failure.
 */
export function unwrap<T>(result: Result<T>): T {
    if (result.success) {
        return result.value;
    } else {
        throw new Error(result.problems.join("\n"));
    }
}

export function result_builder<T>(logger: Logger): ResultBuilder<T> {
    return new ResultBuilder(logger);
}

/**
 * Useful for building up a {@link Result} and logging problems at the same time. Use
 * {@link result_builder} to instantiate.
 */
export class ResultBuilder<T> {
    private readonly problems: Problem[] = [];

    constructor(private readonly logger: Logger) {}

    /**
     * Add a problem to the problems array and log it with {@link logger}.
     */
    add_problem(severity: Severity, ui_message: string, message: string, cause?: any): this {
        this.logger.log(severity, message, cause);
        this.problems.push({ severity, ui_message });
        return this;
    }

    /**
     * Add the given result's problems.
     */
    add_result(result: Result<unknown>): this {
        this.problems.push(...result.problems);
        return this;
    }

    success(value: T): Success<T> {
        return success(value, this.problems);
    }

    failure(): Failure {
        return failure(this.problems);
    }
}
