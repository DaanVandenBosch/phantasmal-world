import { ResizableWidget } from "../../core/gui/ResizableWidget";
import { quest_editor_store } from "../stores/QuestEditorStore";
import { el } from "../../core/gui/dom";
import { REGISTER_COUNT } from "../scripting/vm";
import { TextInput } from "../../core/gui/TextInput";
import { DropDown } from "../../core/gui/DropDown";
import { ToolBar } from "../../core/gui/ToolBar";
import { CheckBox } from "../../core/gui/CheckBox";
import { number_to_hex_string } from "../../core/util";
import "./RegistersView.css";

enum RegisterDisplayType {
    Signed,
    Unsigned,
    Word,
    Byte,
    Float,
}

type RegisterGetterFunction = (register: number) => number;

export class RegistersView extends ResizableWidget {
    private readonly type_dropdown = this.disposable(
        new DropDown(
            "Display type",
            [
                RegisterDisplayType.Signed,
                RegisterDisplayType.Unsigned,
                RegisterDisplayType.Word,
                RegisterDisplayType.Byte,
                RegisterDisplayType.Float,
            ],
            type => RegisterDisplayType[type],
            {
                tooltip: "Select which data type register values should be displayed as.",
            },
        ),
    );
    private register_getter: RegisterGetterFunction = this.get_register_getter(
        RegisterDisplayType.Unsigned,
    );

    private readonly hex_checkbox = this.disposable(
        new CheckBox(false, {
            label: "Hex",
            tooltip: "Display register values in hexadecimal.",
        }),
    );

    private readonly settings_bar = this.disposable(
        new ToolBar({
            children: [this.type_dropdown, this.hex_checkbox],
        }),
    );

    private readonly register_els: TextInput[];
    private readonly list_element = el.div({ class: "quest_editor_RegistersView_list" });
    private readonly container_element = el.div(
        { class: "quest_editor_RegistersView_container" },
        this.list_element,
    );
    public readonly element = el.div(
        { class: "quest_editor_RegistersView" },
        this.settings_bar.element,
        this.container_element,
    );

    constructor() {
        super();

        // create register elements
        const register_els: TextInput[] = Array(REGISTER_COUNT);
        for (let i = 0; i < REGISTER_COUNT; i++) {
            const value_el = this.disposable(
                new TextInput("", {
                    class: "quest_editor_RegistersView_value",
                    label: "r" + i,
                    readonly: true,
                }),
            );

            const wrapper_el = el.div(
                { class: "quest_editor_RegistersView_register" },
                value_el.label!.element,
                value_el.element,
            );

            register_els[i] = value_el;

            this.list_element.appendChild(wrapper_el);
        }
        this.register_els = register_els;

        // predicate that indicates whether to display
        // placeholder text or the actual register values
        const should_use_placeholders = (): boolean =>
            !quest_editor_store.quest_runner.paused.val ||
            !quest_editor_store.quest_runner.running.val;

        // set initial values
        this.update(should_use_placeholders(), this.hex_checkbox.checked.val);

        this.disposables(
            // check if values need to be updated
            // when QuestRunner execution state changes
            quest_editor_store.quest_runner.running.observe(() =>
                this.update(should_use_placeholders(), this.hex_checkbox.checked.val),
            ),
            quest_editor_store.quest_runner.paused.observe(() =>
                this.update(should_use_placeholders(), this.hex_checkbox.checked.val),
            ),

            this.type_dropdown.chosen.observe(change => {
                this.register_getter = this.get_register_getter(change.value);
                this.update(should_use_placeholders(), this.hex_checkbox.checked.val);
            }),

            this.hex_checkbox.checked.observe(change =>
                this.update(should_use_placeholders(), change.value),
            ),
        );

        this.finalize_construction(RegistersView.prototype);
    }

    private get_register_getter(typ: RegisterDisplayType): RegisterGetterFunction {
        let getter: RegisterGetterFunction;

        switch (typ) {
            case RegisterDisplayType.Signed:
                getter = quest_editor_store.quest_runner.vm.get_register_signed;
                break;
            case RegisterDisplayType.Unsigned:
                getter = quest_editor_store.quest_runner.vm.get_register_unsigned;
                break;
            case RegisterDisplayType.Word:
                getter = quest_editor_store.quest_runner.vm.get_register_word;
                break;
            case RegisterDisplayType.Byte:
                getter = quest_editor_store.quest_runner.vm.get_register_byte;
                break;
            case RegisterDisplayType.Float:
                getter = quest_editor_store.quest_runner.vm.get_register_float;
                break;
        }

        return getter.bind(quest_editor_store.quest_runner.vm);
    }

    private update(use_placeholders: boolean, use_hex: boolean): void {
        if (use_placeholders) {
            const placeholder_text = "??";
            for (let i = 0; i < REGISTER_COUNT; i++) {
                const reg_el = this.register_els[i];

                reg_el.value.set_val(placeholder_text, { silent: true });
            }
        } else if (use_hex) {
            for (let i = 0; i < REGISTER_COUNT; i++) {
                const reg_el = this.register_els[i];
                const reg_val = quest_editor_store.quest_runner.vm.get_register_unsigned(i);

                reg_el.value.set_val(number_to_hex_string(reg_val), { silent: true });
            }
        } else {
            for (let i = 0; i < REGISTER_COUNT; i++) {
                const reg_el = this.register_els[i];
                const reg_val = this.register_getter(i);

                reg_el.value.set_val(reg_val.toString(), { silent: true });
            }
        }
    }
}
