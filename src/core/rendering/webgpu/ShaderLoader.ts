import { HttpClient } from "../../HttpClient";

export class ShaderLoader {
    constructor(private readonly http_client: HttpClient) {}

    async load(name: string): Promise<Uint32Array> {
        return new Uint32Array(await this.http_client.get(`/shaders/${name}.spv`).array_buffer());
    }
}
