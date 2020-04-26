// Severities in order of importance.
import { enum_values } from "./enums";
import { assert } from "./util";

export enum Severity {
    Trace,
    Debug,
    Info,
    Warning,
    Error,
    Off,
}

export const Severities: readonly Severity[] = enum_values(Severity);

export function severity_from_string(str: string): Severity {
    const severity = (Severity as any)[str.slice(0, 1).toUpperCase() + str.slice(1).toLowerCase()];
    assert(severity != undefined, () => `"${str}" is not a valid severity.`);
    return severity;
}
