import { load_array_buffer } from "../../core/loading";

export async function get_character_class_data(
    player_class: string,
    body_part: string,
    no?: number,
): Promise<ArrayBuffer> {
    return await load_array_buffer(character_class_to_url(player_class, body_part, no));
}

export async function get_character_class_animation_data(
    animation_id: number,
): Promise<ArrayBuffer> {
    return await load_array_buffer(
        `/player/animation/animation_${animation_id.toString().padStart(3, "0")}.njm`,
    );
}

function character_class_to_url(player_class: string, body_part: string, no?: number): string {
    return `/player/${player_class}${body_part}${no == null ? "" : no}.nj`;
}
