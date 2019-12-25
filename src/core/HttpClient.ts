import { DisposablePromise } from "./DisposablePromise";

export interface HttpClient {
    get(url: string): HttpResponse;
}

export interface HttpResponse {
    json<T>(): DisposablePromise<T>;

    array_buffer(): DisposablePromise<ArrayBuffer>;
}

/**
 * This client uses {@link fetch}.
 */
export class FetchClient implements HttpClient {
    get(url: string): HttpResponse {
        const aborter = new AbortController();
        const response = fetch(process.env.PUBLIC_URL + url, { signal: aborter.signal });
        return {
            json<T>(): DisposablePromise<T> {
                return new DisposablePromise(
                    (resolve, reject) => {
                        response
                            .then(r => r.json())
                            .then(
                                json => resolve(json),
                                error => reject(error),
                            );
                    },
                    () => aborter.abort(),
                );
            },

            array_buffer(): DisposablePromise<ArrayBuffer> {
                return new DisposablePromise(
                    (resolve, reject) => {
                        response
                            .then(r => r.arrayBuffer())
                            .then(
                                buf => resolve(buf),
                                error => reject(error),
                            );
                    },
                    () => aborter.abort(),
                );
            },
        };
    }
}

/**
 * This client simple throws an error when used.
 */
export class StubHttpClient implements HttpClient {
    get(url: string): HttpResponse {
        return {
            json<T>(): DisposablePromise<T> {
                throw new Error(`Stub client's json method invoked for get request to "${url}".`);
            },

            array_buffer(): DisposablePromise<ArrayBuffer> {
                throw new Error(
                    `Stub client's array_buffer method invoked for get request to "${url}".`,
                );
            },
        };
    }
}
