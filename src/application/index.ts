import { HttpClient } from "../core/HttpClient";
import { Disposable } from "../core/observable/Disposable";
import { GuiStore, GuiTool } from "../core/stores/GuiStore";
import { create_item_type_stores } from "../core/stores/ItemTypeStore";
import { create_item_drop_stores } from "../hunt_optimizer/stores/ItemDropStore";
import { ApplicationView } from "./gui/ApplicationView";
import { throttle } from "lodash";
import { DisposableThreeRenderer } from "../core/rendering/Renderer";
import { Disposer } from "../core/observable/Disposer";
import { disposable_custom_listener, disposable_listener } from "../core/gui/dom";
import { Random } from "../core/Random";
import { NavigationController } from "./controllers/NavigationController";
import { NavigationView } from "./gui/NavigationView";
import { MainContentView } from "./gui/MainContentView";
import { Clock } from "../core/Clock";

export function initialize_application(
    http_client: HttpClient,
    random: Random,
    clock: Clock,
    create_three_renderer: () => DisposableThreeRenderer,
): Disposable {
    const disposer = new Disposer();

    // Disable native undo/redo.
    disposer.add(disposable_custom_listener(document, "beforeinput", before_input));
    // Work-around for FireFox:
    disposer.add(disposable_listener(document, "keydown", keydown));

    // Disable native drag-and-drop to avoid users dragging in unsupported file formats and leaving
    // the application unexpectedly.
    disposer.add_all(
        disposable_listener(document, "dragenter", dragenter),
        disposable_listener(document, "dragover", dragover),
        disposable_listener(document, "drop", drop),
    );

    // Initialize core stores shared by several submodules.
    const gui_store = disposer.add(new GuiStore());
    const item_type_stores = disposer.add(create_item_type_stores(http_client, gui_store));
    const item_drop_stores = disposer.add(
        create_item_drop_stores(http_client, gui_store, item_type_stores),
    );

    // Controllers.
    const navigation_controller = disposer.add(new NavigationController(gui_store, clock));

    // Initialize application view.
    const application_view = disposer.add(
        new ApplicationView(
            new NavigationView(navigation_controller),
            new MainContentView(gui_store, [
                [
                    GuiTool.Viewer,
                    async () => {
                        const { initialize_viewer } = await import("../viewer");
                        const viewer = disposer.add(
                            initialize_viewer(
                                http_client,
                                random,
                                gui_store,
                                create_three_renderer,
                            ),
                        );

                        return viewer.view;
                    },
                ],
                [
                    GuiTool.QuestEditor,
                    async () => {
                        const { initialize_quest_editor } = await import("../quest_editor");
                        const quest_editor = disposer.add(
                            initialize_quest_editor(http_client, gui_store, create_three_renderer),
                        );

                        return quest_editor.view;
                    },
                ],
                [
                    GuiTool.HuntOptimizer,
                    async () => {
                        const { initialize_hunt_optimizer } = await import("../hunt_optimizer");
                        const hunt_optimizer = disposer.add(
                            initialize_hunt_optimizer(
                                http_client,
                                gui_store,
                                item_type_stores,
                                item_drop_stores,
                            ),
                        );

                        return hunt_optimizer.view;
                    },
                ],
            ]),
        ),
    );

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
    application_view.activate();

    disposer.add(disposable_listener(window, "resize", resize));

    return {
        dispose(): void {
            disposer.dispose();
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
