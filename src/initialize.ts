import { HttpClient } from "./core/HttpClient";
import { Disposable } from "./core/observable/Disposable";
import { GuiStore, GuiTool } from "./core/stores/GuiStore";
import { create_item_type_stores } from "./core/stores/ItemTypeStore";
import { create_item_drop_stores } from "./hunt_optimizer/stores/ItemDropStore";
import { ApplicationView } from "./application/gui/ApplicationView";
import { throttle } from "lodash";
import { DisposableThreeRenderer } from "./core/rendering/Renderer";

export function initialize(
    http_client: HttpClient,
    create_three_renderer: () => DisposableThreeRenderer,
): Disposable {
    // Disable native undo/redo.
    document.addEventListener("beforeinput", before_input);
    // Work-around for FireFox:
    document.addEventListener("keydown", keydown);

    // Disable native drag-and-drop.
    document.addEventListener("dragenter", dragenter);
    document.addEventListener("dragover", dragover);
    document.addEventListener("drop", drop);

    // Initialize core stores shared by several submodules.
    const gui_store = new GuiStore();
    const item_type_stores = create_item_type_stores(http_client, gui_store);
    const item_drop_stores = create_item_drop_stores(http_client, gui_store, item_type_stores);

    // Initialize application view.
    const application_view = new ApplicationView(gui_store, [
        [
            GuiTool.Viewer,
            async () => {
                return (await import("./viewer/index")).initialize_viewer(
                    http_client,
                    gui_store,
                    create_three_renderer,
                );
            },
        ],
        [
            GuiTool.QuestEditor,
            async () => {
                return (await import("./quest_editor/index")).initialize_quest_editor(
                    http_client,
                    gui_store,
                    create_three_renderer,
                );
            },
        ],
        [
            GuiTool.HuntOptimizer,
            async () => {
                return (await import("./hunt_optimizer/index")).initialize_hunt_optimizer(
                    http_client,
                    gui_store,
                    item_type_stores,
                    item_drop_stores,
                );
            },
        ],
    ]);

    // Resize the view on window resize.
    const resize = throttle(
        () => {
            application_view.resize(window.innerWidth, window.innerHeight);
        },
        100,
        { leading: true, trailing: true },
    );

    resize();
    document.body.append(application_view.element);
    window.addEventListener("resize", resize);

    // Dispose view and global event listeners when necessary.
    return {
        dispose(): void {
            window.removeEventListener("beforeinput", before_input);
            window.removeEventListener("keydown", keydown);
            window.removeEventListener("resize", resize);
            window.removeEventListener("dragenter", dragenter);
            window.removeEventListener("dragover", dragover);
            window.removeEventListener("drop", drop);
            application_view.dispose();
        },
    };
}

function before_input(e: Event): void {
    const ie = e as any;

    if (ie.inputType === "historyUndo" || ie.inputType === "historyRedo") {
        e.preventDefault();
    }
}

function keydown(e: Event): void {
    const kbe = e as KeyboardEvent;

    if (kbe.ctrlKey && !kbe.altKey && kbe.key.toUpperCase() === "Z") {
        kbe.preventDefault();
    }
}

function dragenter(e: DragEvent): void {
    e.preventDefault();

    if (e.dataTransfer) {
        e.dataTransfer.dropEffect = "none";
    }
}

function dragover(e: DragEvent): void {
    dragenter(e);
}

function drop(e: DragEvent): void {
    dragenter(e);
}
