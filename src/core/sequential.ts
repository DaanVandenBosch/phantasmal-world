/**
 * Takes a function f that returns a promise and returns a function that forwards calls to f
 * sequentially. So f will never be called while a call to f is underway.
 */
export function sequential<F extends (...args: any[]) => Promise<any>>(f: F): F {
    const queue: {
        args: any[];
        resolve: (value: any) => void;
        reject: (reason: any) => void;
    }[] = [];

    async function process_queue(): Promise<void> {
        while (queue.length) {
            const { args, resolve, reject } = queue[0];

            try {
                resolve(await f(...args));
            } catch (e) {
                reject(e);
            } finally {
                queue.shift();
            }
        }
    }

    function g(...args: any[]): Promise<any> {
        const promise = new Promise((resolve, reject) => queue.push({ args, resolve, reject }));

        if (queue.length === 1) {
            process_queue();
        }

        return promise;
    }

    return g as F;
}
