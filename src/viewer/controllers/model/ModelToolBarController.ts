import { Controller } from "../../../core/controllers/Controller";
import { Property } from "../../../core/observable/property/Property";
import { ModelStore } from "../../stores/ModelStore";
import { read_file } from "../../../core/files";
import { ArrayBufferCursor } from "../../../core/data_formats/block/cursor/ArrayBufferCursor";
import { Endianness } from "../../../core/data_formats/block/Endianness";
import { parse_nj, parse_xj } from "../../../core/data_formats/parsing/ninja";
import { parse_njm } from "../../../core/data_formats/parsing/ninja/motion";
import { LogManager } from "../../../core/Logger";
import { failure, problem, Result, success } from "../../../core/Result";
import { Severity } from "../../../core/Severity";
import { property } from "../../../core/observable";
import { WritableProperty } from "../../../core/observable/property/WritableProperty";
import { parse_afs_textures, parse_xvm_textures } from "../../util/texture_parsing";

const logger = LogManager.get("viewer/controllers/model/ModelToolBarController");

export class ModelToolBarController extends Controller {
    private readonly _result_dialog_visible = property(false);
    private readonly _result: WritableProperty<Result<unknown> | undefined> = property(undefined);
    private readonly _result_problems_message = property("");
    private readonly _result_error_message = property("");

    readonly show_skeleton: Property<boolean>;
    readonly animation_frame_count: Property<number>;
    readonly animation_frame_count_label: Property<string>;
    readonly animation_controls_enabled: Property<boolean>;
    readonly animation_playing: Property<boolean>;
    readonly animation_frame_rate: Property<number>;
    readonly animation_frame: Property<number>;

    readonly result_dialog_visible: Property<boolean> = this._result_dialog_visible;
    readonly result: Property<Result<unknown> | undefined> = this._result;
    readonly result_problems_message: Property<string> = this._result_problems_message;
    readonly result_error_message: Property<string> = this._result_error_message;

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
        this._result_problems_message.val = `Encountered some problems while opening "${file.name}".`;
        this._result_error_message.val = `Couldn't open "${file.name}".`;

        try {
            const buffer = await read_file(file);
            const cursor = new ArrayBufferCursor(buffer, Endianness.Little);

            if (file.name.endsWith(".nj")) {
                const nj_result = parse_nj(cursor);
                this.set_result(nj_result);

                if (nj_result.success) {
                    this.store.set_current_nj_object(nj_result.value[0]);
                }
            } else if (file.name.endsWith(".xj")) {
                const xj_result = parse_xj(cursor);
                this.set_result(xj_result);

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
                    this.set_result(success(undefined));
                } else {
                    this.set_result(failure(problem(Severity.Error, "No model to animate")));
                }
            } else if (file.name.endsWith(".xvm")) {
                const xvm_result = parse_xvm_textures(cursor);
                this.set_result(xvm_result);
                this.store.set_current_textures(xvm_result.value ?? []);
            } else if (file.name.endsWith(".afs")) {
                const afs_result = parse_afs_textures(cursor);
                this.set_result(afs_result);
                this.store.set_current_textures(afs_result.value ?? []);
            } else {
                logger.debug(`Unsupported file extension in filename "${file.name}".`);
                this.set_result(failure(problem(Severity.Error, "Unsupported file type.")));
            }
        } catch (e) {
            logger.error("Couldn't read file.", e);
            this.set_result(failure());
        }
    };

    dismiss_result_dialog = (): void => {
        this._result_dialog_visible.val = false;
    };

    private set_result(result: Result<unknown>): void {
        this._result.val = result;

        if (result.problems.length) {
            this._result_dialog_visible.val = true;
        }
    }
}
