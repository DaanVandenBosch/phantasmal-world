import { QuestEditorView } from "./gui/QuestEditorView";
import { GuiStore } from "../core/stores/GuiStore";
import { QuestEditorStore } from "./stores/QuestEditorStore";
import { AsmEditorStore } from "./stores/AsmEditorStore";
import { AreaStore } from "./stores/AreaStore";
import { AreaAssetLoader } from "./loading/AreaAssetLoader";
import { HttpClient } from "../core/HttpClient";
import { EntityImageRenderer } from "./rendering/EntityImageRenderer";
import { EntityAssetLoader } from "./loading/EntityAssetLoader";
import { DisposableThreeRenderer } from "../core/rendering/Renderer";
import { QuestEditorUiPersister } from "./persistence/QuestEditorUiPersister";
import { QuestEditorToolBarView } from "./gui/QuestEditorToolBarView";
import { QuestEditorToolBarController } from "./controllers/QuestEditorToolBarController";
import { QuestInfoView } from "./gui/QuestInfoView";
import { NpcCountsView } from "./gui/NpcCountsView";
import { QuestEditorRendererView } from "./gui/QuestEditorRendererView";
import { AsmEditorView } from "./gui/AsmEditorView";
import { EntityInfoView } from "./gui/EntityInfoView";
import { NpcListView } from "./gui/NpcListView";
import { ObjectListView } from "./gui/ObjectListView";
import { EventsView } from "./gui/EventsView";
import { QuestRunnerRendererView } from "./gui/QuestRunnerRendererView";
import { RegistersView } from "./gui/RegistersView";
import { QuestInfoController } from "./controllers/QuestInfoController";
import { Disposer } from "../core/observable/Disposer";
import { Disposable } from "../core/observable/Disposable";
import { EntityInfoController } from "./controllers/EntityInfoController";
import { NpcCountsController } from "./controllers/NpcCountsController";
import { EventsController } from "./controllers/EventsController";
import { DebugView } from "./gui/DebugView";
import { DebugController } from "./controllers/DebugController";
import { LogStore } from "./stores/LogStore";

export function initialize_quest_editor(
    http_client: HttpClient,
    gui_store: GuiStore,
    create_three_renderer: () => DisposableThreeRenderer,
): { view: QuestEditorView } & Disposable {
    const disposer = new Disposer();

    // Asset Loaders
    const area_asset_loader = disposer.add(new AreaAssetLoader(http_client));
    const entity_asset_loader = disposer.add(new EntityAssetLoader(http_client));

    // Stores
    const area_store = disposer.add(new AreaStore(area_asset_loader));
    const log_store = disposer.add(new LogStore());
    const quest_editor_store = disposer.add(new QuestEditorStore(gui_store, area_store, log_store));
    const asm_editor_store = disposer.add(new AsmEditorStore(quest_editor_store));

    // Persisters
    const quest_editor_ui_persister = new QuestEditorUiPersister();

    // Entity Image Renderer
    const entity_image_renderer = disposer.add(
        new EntityImageRenderer(entity_asset_loader, create_three_renderer),
    );

    // View
    const view = disposer.add(
        new QuestEditorView(
            gui_store,
            quest_editor_store,
            quest_editor_ui_persister,
            disposer.add(
                new QuestEditorToolBarView(
                    disposer.add(
                        new QuestEditorToolBarController(gui_store, area_store, quest_editor_store),
                    ),
                ),
            ),
            () => new QuestInfoView(disposer.add(new QuestInfoController(quest_editor_store))),
            () => new NpcCountsView(disposer.add(new NpcCountsController(quest_editor_store))),
            () =>
                new QuestEditorRendererView(
                    quest_editor_store,
                    area_asset_loader,
                    entity_asset_loader,
                    create_three_renderer(),
                ),
            () => new AsmEditorView(gui_store, quest_editor_store.quest_runner, asm_editor_store),
            () => new EntityInfoView(disposer.add(new EntityInfoController(quest_editor_store))),
            () => new NpcListView(quest_editor_store, entity_image_renderer),
            () => new ObjectListView(quest_editor_store, entity_image_renderer),
            () => new EventsView(disposer.add(new EventsController(quest_editor_store))),
            () =>
                new QuestRunnerRendererView(
                    quest_editor_store,
                    area_asset_loader,
                    entity_asset_loader,
                    create_three_renderer(),
                ),
            () =>
                new DebugView(
                    disposer.add(new DebugController(gui_store, quest_editor_store, log_store)),
                ),
            () => new RegistersView(quest_editor_store.quest_runner),
        ),
    );

    return {
        view,
        dispose() {
            disposer.dispose();
        },
    };
}
