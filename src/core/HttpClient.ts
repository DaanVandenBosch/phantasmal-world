export interface HttpClient {
    get(url: string): HttpResponse;
}

export interface HttpResponse {
    json<T>(): Promise<T>;

    array_buffer(): Promise<ArrayBuffer>;
}

/**
 * This client uses {@link fetch}.
 */
export class FetchClient implements HttpClient {
    get(url: string): HttpResponse {
        const response = fetch(process.env.PUBLIC_URL + url);
        return {
            async json<T>(): Promise<T> {
                return (await response).json();
            },

            async array_buffer(): Promise<ArrayBuffer> {
                return (await response).arrayBuffer();
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
            json<T>(): Promise<T> {
                throw new Error(`Stub client's json method invoked for get request to "${url}".`);
            },

            array_buffer(): Promise<ArrayBuffer> {
                throw new Error(
                    `Stub client's array_buffer method invoked for get request to "${url}".`,
                );
            },
        };
    }
}
