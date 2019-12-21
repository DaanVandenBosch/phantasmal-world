import { HttpClient } from "../../core/HttpClient";

export class CharacterClassAssetLoader {
    constructor(private readonly http_client: HttpClient) {}

    async load_geometry(
        player_class: string,
        body_part: string,
        no?: number,
    ): Promise<ArrayBuffer> {
        return await this.http_client
            .get(character_class_to_url(player_class, body_part, no))
            .array_buffer();
    }

    async load_animation(animation_id: number): Promise<ArrayBuffer> {
        return await this.http_client
            .get(`/player/animation/animation_${animation_id.toString().padStart(3, "0")}.njm`)
            .array_buffer();
    }
}

function character_class_to_url(player_class: string, body_part: string, no?: number): string {
    return `/player/${player_class}${body_part}${no == null ? "" : no}.nj`;
}
