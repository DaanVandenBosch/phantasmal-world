import { sequential } from "./sequential";

test("sequential functions should run sequentially", () => {
    let time = 10;
    const f = sequential(() => new Promise(resolve => setTimeout(resolve, time--)));

    const resolved_values: number[] = [];
    let last_promise!: Promise<any>;

    for (let i = 0; i < 10; i++) {
        last_promise = f().then(() => resolved_values.push(i));
    }

    expect(resolved_values).toEqual([]);

    return last_promise.then(() => expect(resolved_values).toEqual([0, 1, 2, 3, 4, 5, 6, 7, 8, 9]));
});
