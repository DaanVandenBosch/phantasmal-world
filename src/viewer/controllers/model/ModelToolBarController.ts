import { Controller } from "../../../core/controllers/Controller";
import { Property } from "../../../core/observable/property/Property";
import { ModelStore } from "../../stores/ModelStore";
import { read_file } from "../../../core/read_file";
import { ArrayBufferCursor } from "../../../core/data_formats/cursor/ArrayBufferCursor";
import { Endianness } from "../../../core/data_formats/Endianness";
import { parse_nj, parse_xj } from "../../../core/data_formats/parsing/ninja";
import { parse_njm } from "../../../core/data_formats/parsing/ninja/motion";
import { parse_xvm, XvrTexture } from "../../../core/data_formats/parsing/ninja/texture";
import { parse_afs } from "../../../core/data_formats/parsing/afs";
import { LogManager } from "../../../core/Logger";

const logger = LogManager.get("viewer/controllers/model/ModelToolBarController");

export class ModelToolBarController extends Controller {
    readonly show_skeleton: Property<boolean>;
    readonly animation_frame_count: Property<number>;
    readonly animation_frame_count_label: Property<string>;
    readonly animation_controls_enabled: Property<boolean>;
    readonly animation_playing: Property<boolean>;
    readonly animation_frame_rate: Property<number>;
    readonly animation_frame: Property<number>;

    constructor(private readonly store: ModelStore) {
        super();

        this.show_skeleton = store.show_skeleton;
        this.animation_frame_count = store.animation_frame_count;
        this.animation_frame_count_label = store.animation_frame_count.map(count => `/ ${count}`);
        this.animation_controls_enabled = store.current_nj_motion.map(njm => njm != undefined);
        this.animation_playing = store.animation_playing;
        this.animation_frame_rate = store.animation_frame_rate;
        this.animation_frame = store.animation_frame.map(v => Math.round(v));
    }

    set_show_skeleton = (show_skeleton: boolean): void => {
        this.store.set_show_skeleton(show_skeleton);
    };

    set_animation_playing = (playing: boolean): void => {
        this.store.set_animation_playing(playing);
    };

    set_animation_frame_rate = (frame_rate: number): void => {
        this.store.set_animation_frame_rate(frame_rate);
    };

    set_animation_frame = (frame: number): void => {
        this.store.set_animation_frame(frame);
    };

    // TODO: notify user of problems.
    load_file = async (file: File): Promise<void> => {
        try {
            const buffer = await read_file(file);
            const cursor = new ArrayBufferCursor(buffer, Endianness.Little);

            if (file.name.endsWith(".nj")) {
                this.store.set_current_nj_object(parse_nj(cursor)[0]);
            } else if (file.name.endsWith(".xj")) {
                this.store.set_current_nj_object(parse_xj(cursor)[0]);
            } else if (file.name.endsWith(".njm")) {
                this.store.set_current_animation(undefined);
                this.store.set_current_nj_motion(undefined);

                const nj_object = this.store.current_nj_object.val;

                if (nj_object) {
                    this.set_animation_playing(true);
                    this.store.set_current_nj_motion(parse_njm(cursor, nj_object.bone_count()));
                }
            } else if (file.name.endsWith(".xvm")) {
                this.store.set_current_textures(parse_xvm(cursor).textures);
            } else if (file.name.endsWith(".afs")) {
                const files = parse_afs(cursor);
                const textures: XvrTexture[] = files.flatMap(
                    file => parse_xvm(new ArrayBufferCursor(file, Endianness.Little)).textures,
                );

                this.store.set_current_textures(textures);
            } else {
                logger.error(`Unknown file extension in filename "${file.name}".`);
            }
        } catch (e) {
            logger.error("Couldn't read file.", e);
        }
    };
}
