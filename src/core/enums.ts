// eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
export function enum_values<E>(e: any): E[] {
    const values = Object.values(e);
    const number_values = values.filter(v => typeof v === "number");

    if (number_values.length) {
        return (number_values as any) as E[];
    } else {
        return (values as any) as E[];
    }
}

// eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
export function string_to_enum<E>(e: any, str: string): E | undefined {
    if (str === "") return undefined;

    // Filter out strings that start with a digit to avoid index `e` with a number string which
    // could result in return a string.
    const first_char_code = str.charCodeAt(0);
    if (48 <= first_char_code && first_char_code <= 57) return undefined;

    return e[str];
}
