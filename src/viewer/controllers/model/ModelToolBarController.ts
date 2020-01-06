import { Controller } from "../../../core/controllers/Controller";
import { Property } from "../../../core/observable/property/Property";
import { ModelStore } from "../../stores/ModelStore";
import { read_file } from "../../../core/files";
import { ArrayBufferCursor } from "../../../core/data_formats/cursor/ArrayBufferCursor";
import { Endianness } from "../../../core/data_formats/Endianness";
import { parse_nj, parse_xj } from "../../../core/data_formats/parsing/ninja";
import { parse_njm } from "../../../core/data_formats/parsing/ninja/motion";
import { is_xvm, parse_xvm, XvrTexture } from "../../../core/data_formats/parsing/ninja/texture";
import { parse_afs } from "../../../core/data_formats/parsing/afs";
import { LogManager } from "../../../core/Logger";
import { prs_decompress } from "../../../core/data_formats/compression/prs/decompress";
import { failure, Result, result_builder, success } from "../../../core/Result";
import { show_result_popup } from "../../../core/gui/ResultPopup";
import { Severity } from "../../../core/Severity";

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

    load_file = async (file: File): Promise<void> => {
        let result: Result<unknown>;

        try {
            const buffer = await read_file(file);
            const cursor = new ArrayBufferCursor(buffer, Endianness.Little);

            if (file.name.endsWith(".nj")) {
                const nj_result = (result = parse_nj(cursor));

                if (nj_result.success) {
                    this.store.set_current_nj_object(nj_result.value[0]);
                }
            } else if (file.name.endsWith(".xj")) {
                const xj_result = (result = parse_xj(cursor));

                if (xj_result.success) {
                    this.store.set_current_nj_object(xj_result.value[0]);
                }
            } else if (file.name.endsWith(".njm")) {
                this.store.set_current_animation(undefined);
                this.store.set_current_nj_motion(undefined);

                const nj_object = this.store.current_nj_object.val;

                if (nj_object) {
                    this.set_animation_playing(true);
                    this.store.set_current_nj_motion(parse_njm(cursor, nj_object.bone_count()));
                    result = success(undefined);
                } else {
                    result = failure([
                        { severity: Severity.Error, ui_message: "No model to animate" },
                    ]);
                }
            } else if (file.name.endsWith(".xvm")) {
                const xvm_result = (result = parse_xvm(cursor));

                if (xvm_result.success) {
                    this.store.set_current_textures(xvm_result.value.textures);
                } else {
                    this.store.set_current_textures([]);
                }
            } else if (file.name.endsWith(".afs")) {
                const rb = result_builder(logger);
                const afs_result = parse_afs(cursor);
                rb.add_result(afs_result);

                if (!afs_result.success) {
                    result = rb.failure();
                } else {
                    const textures: XvrTexture[] = afs_result.value.flatMap(file => {
                        const cursor = new ArrayBufferCursor(file, Endianness.Little);

                        if (is_xvm(cursor)) {
                            const xvm_result = parse_xvm(cursor);
                            rb.add_result(xvm_result);
                            return xvm_result.value?.textures ?? [];
                        } else {
                            const xvm_result = parse_xvm(prs_decompress(cursor.seek_start(0)));
                            rb.add_result(xvm_result);
                            return xvm_result.value?.textures ?? [];
                        }
                    });

                    if (textures.length) {
                        result = rb.success(textures);
                    } else {
                        result = rb.failure();
                    }

                    this.store.set_current_textures(textures);
                }
            } else {
                logger.debug(`Unsupported file extension in filename "${file.name}".`);
                result = failure([
                    { severity: Severity.Error, ui_message: "Unsupported file type." },
                ]);
            }
        } catch (e) {
            logger.error("Couldn't read file.", e);
            result = failure();
        }

        show_result_popup(
            result,
            `Encountered some problems while opening "${file.name}".`,
            `Couldn't open "${file.name}".`,
        );
    };
}
