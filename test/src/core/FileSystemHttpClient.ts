import { HttpClient, HttpResponse } from "../../../src/core/HttpClient";
import * as fs from "fs";

export class FileSystemHttpClient implements HttpClient {
    get(url: string): HttpResponse {
        return {
            async json<T>(): Promise<T> {
                throw new Error(
                    `FileSystemHttpClient's json method invoked for get request to "${url}".`,
                );
            },

            async array_buffer(): Promise<ArrayBuffer> {
                const buf = await fs.promises.readFile(`./assets${url}`);
                return buf.buffer.slice(buf.byteOffset, buf.byteOffset + buf.byteLength);
            },
        };
    }
}
