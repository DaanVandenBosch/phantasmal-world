import { HttpClient, HttpResponse } from "../../../src/core/HttpClient";
import * as fs from "fs";
import { DisposablePromise } from "../../../src/core/DisposablePromise";

export class FileSystemHttpClient implements HttpClient {
    get(url: string): HttpResponse {
        return {
            json<T>(): DisposablePromise<T> {
                return DisposablePromise.resolve(fs.promises.readFile(`./assets${url}`)).then(buf =>
                    JSON.parse(buf.toString()),
                );
            },

            array_buffer(): DisposablePromise<ArrayBuffer> {
                return DisposablePromise.resolve(fs.promises.readFile(`./assets${url}`)).then(buf =>
                    buf.buffer.slice(buf.byteOffset, buf.byteOffset + buf.byteLength),
                );
            },
        };
    }
}
