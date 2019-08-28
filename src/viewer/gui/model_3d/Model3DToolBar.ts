import { ToolBar } from "../../../core/gui/ToolBar";
import { FileButton } from "../../../core/gui/FileButton";
import { CheckBox } from "../../../core/gui/CheckBox";
import { NumberInput } from "../../../core/gui/NumberInput";
import { PSO_FRAME_RATE } from "../../../core/rendering/conversion/ninja_animation";
import { model_store } from "../../stores/Model3DStore";
import { Label } from "../../../core/gui/Label";

export class Model3DToolBar extends ToolBar {
    constructor() {
        const open_file_button = new FileButton("Open file...", ".nj, .njm, .xj, .xvm");
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
            max: model_store.animation_frame_count,
            step: 1,
        });
        const animation_frame_count_label = new Label(
            model_store.animation_frame_count.map(count => `/ ${count}`),
        );

        super({
            children: [
                open_file_button,
                skeleton_checkbox,
                play_animation_checkbox,
                animation_frame_rate_input,
                animation_frame_input,
                animation_frame_count_label,
            ],
        });

        // Always-enabled controls.
        this.disposables(
            open_file_button.files.observe(({ value: files }) => {
                if (files.length) model_store.load_file(files[0]);
            }),

            model_store.show_skeleton.bind_to(skeleton_checkbox.checked),
        );

        // Controls that are only enabled when an animation is selected.
        const enabled = model_store.current_nj_motion.map(njm => njm != undefined);

        this.disposables(
            play_animation_checkbox.enabled.bind_to(enabled),
            model_store.animation_playing.bind_bi(play_animation_checkbox.checked),

            animation_frame_rate_input.enabled.bind_to(enabled),
            model_store.animation_frame_rate.bind_to(animation_frame_rate_input.value),

            animation_frame_input.enabled.bind_to(enabled),
            model_store.animation_frame.bind_to(animation_frame_input.value),
            animation_frame_input.value.bind_to(
                model_store.animation_frame.map(v => Math.round(v)),
            ),

            animation_frame_count_label.enabled.bind_to(enabled),
        );
    }
}
