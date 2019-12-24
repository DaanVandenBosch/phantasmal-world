import { HttpClient, HttpResponse } from "../../../src/core/HttpClient";
import * as fs from "fs";

export class FileSystemHttpClient implements HttpClient {
    get(url: string): HttpResponse {
        return {
            async json<T>(): Promise<T> {
                const buf = await fs.promises.readFile(`./assets${url}`);
                return JSON.parse(buf.toString());
            },

            async array_buffer(): Promise<ArrayBuffer> {
                const buf = await fs.promises.readFile(`./assets${url}`);
                return buf.buffer.slice(buf.byteOffset, buf.byteOffset + buf.byteLength);
            },
        };
    }
}
