import { try_finally } from "./util";

test("try_finally with synchronous function", () => {
    let after_called = false;

    const value = try_finally(
        () => 999,
        () => (after_called = true),
    );

    expect(value).toBe(999);
    expect(after_called).toBe(true);
});

test("try_finally with asynchronous function", async () => {
    let after_called = false;

    const value = await try_finally(
        async () => 567,
        () => (after_called = true),
    );

    expect(value).toBe(567);
    expect(after_called).toBe(true);
});

test("try_finally with promise-returning function", async () => {
    let after_called = false;

    const value = await try_finally(
        () => new Promise(resolve => setTimeout(() => resolve(849), 10)),
        () => (after_called = true),
    );

    expect(value).toBe(849);
    expect(after_called).toBe(true);
});
