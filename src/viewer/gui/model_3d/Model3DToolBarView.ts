import { ToolBar } from "../../../core/gui/ToolBar";
import { FileButton } from "../../../core/gui/FileButton";
import { CheckBox } from "../../../core/gui/CheckBox";
import { NumberInput } from "../../../core/gui/NumberInput";
import { PSO_FRAME_RATE } from "../../../core/rendering/conversion/ninja_animation";
import { Label } from "../../../core/gui/Label";
import { Icon } from "../../../core/gui/dom";
import { Model3DStore } from "../../stores/Model3DStore";
import { View } from "../../../core/gui/View";

export class Model3DToolBarView extends View {
    private readonly toolbar: ToolBar;

    get element(): HTMLElement {
        return this.toolbar.element;
    }

    get height(): number {
        return this.toolbar.height;
    }

    constructor(model_3d_store: Model3DStore) {
        super();

        const open_file_button = new FileButton("Open file...", {
            icon_left: Icon.File,
            accept: ".afs, .nj, .njm, .xj, .xvm",
        });
        const skeleton_checkbox = new CheckBox(false, { label: "Show skeleton" });
        const play_animation_checkbox = new CheckBox(true, { label: "Play animation" });
        const animation_frame_rate_input = new NumberInput(PSO_FRAME_RATE, {
            label: "Frame rate:",
            min: 1,
            max: 240,
            step: 1,
        });
        const animation_frame_input = new NumberInput(1, {
            label: "Frame:",
            min: 1,
            max: model_3d_store.animation_frame_count,
            step: 1,
        });
        const animation_frame_count_label = new Label(
            model_3d_store.animation_frame_count.map(count => `/ ${count}`),
        );

        this.toolbar = this.add(
            new ToolBar(
                open_file_button,
                skeleton_checkbox,
                play_animation_checkbox,
                animation_frame_rate_input,
                animation_frame_input,
                animation_frame_count_label,
            ),
        );

        // Always-enabled controls.
        this.disposables(
            open_file_button.files.observe(({ value: files }) => {
                if (files.length) model_3d_store.load_file(files[0]);
            }),

            skeleton_checkbox.checked.observe(({ value }) =>
                model_3d_store.set_show_skeleton(value),
            ),
        );

        // Controls that are only enabled when an animation is selected.
        const enabled = model_3d_store.current_nj_motion.map(njm => njm != undefined);

        this.disposables(
            play_animation_checkbox.enabled.bind_to(enabled),
            play_animation_checkbox.checked.bind_to(model_3d_store.animation_playing),
            play_animation_checkbox.checked.observe(({ value }) =>
                model_3d_store.set_animation_playing(value),
            ),

            animation_frame_rate_input.enabled.bind_to(enabled),
            animation_frame_rate_input.value.observe(({ value }) =>
                model_3d_store.set_animation_frame_rate(value),
            ),

            animation_frame_input.enabled.bind_to(enabled),
            animation_frame_input.value.bind_to(
                model_3d_store.animation_frame.map(v => Math.round(v)),
            ),
            animation_frame_input.value.observe(({ value }) =>
                model_3d_store.set_animation_frame(value),
            ),

            animation_frame_count_label.enabled.bind_to(enabled),
        );

        this.finalize_construction();
    }
}
