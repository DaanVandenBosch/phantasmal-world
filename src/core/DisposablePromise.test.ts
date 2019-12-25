import { DisposablePromise } from "./DisposablePromise";
import { timeout } from "../../test/src/utils";

test("It should resolve correctly.", () => {
    return new DisposablePromise((resolve, reject) => {
        resolve(700);
        resolve(800);
        reject(new Error());
        resolve(900);
        reject(new Error());
    }).then(
        x => {
            expect(x).toBe(700);
        },
        () => {
            throw new Error("Should never be called.");
        },
    );
});

test("It should reject correctly.", () => {
    return new DisposablePromise((resolve, reject) => {
        reject(new Error("ERROR"));
        resolve(700);
        resolve(800);
        reject(new Error());
        resolve(900);
        reject(new Error());
    }).then(
        () => {
            throw new Error("Should never be called.");
        },
        err => {
            expect((err as Error).message).toBe("ERROR");
        },
    );
});

test("It should dispose correctly.", async () => {
    let resolve: (value: number) => void;
    let value = 7;
    let cancel_called = false;

    const promise = new DisposablePromise<number>(
        r => {
            resolve = r;
        },
        () => {
            cancel_called = true;
        },
    );

    await timeout(0);

    promise.dispose();

    expect(cancel_called).toBe(true);

    resolve!(13);

    promise.then(
        v => {
            value = v;
        },
        () => {
            throw new Error("Should never be called.");
        },
    );

    await timeout(0);

    expect(value).toBe(7);
});
