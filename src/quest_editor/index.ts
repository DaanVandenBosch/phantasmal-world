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
import { QuestEditorToolBar } from "./gui/QuestEditorToolBar";
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

export function initialize_quest_editor(
    http_client: HttpClient,
    gui_store: GuiStore,
    create_three_renderer: () => DisposableThreeRenderer,
): QuestEditorView {
    // Asset Loaders
    const area_asset_loader = new AreaAssetLoader(http_client);
    const entity_asset_loader = new EntityAssetLoader(http_client);

    // Stores
    const area_store = new AreaStore(area_asset_loader);
    const quest_editor_store = new QuestEditorStore(gui_store, area_store);
    const asm_editor_store = new AsmEditorStore(quest_editor_store);

    // Persisters
    const quest_editor_ui_persister = new QuestEditorUiPersister();

    // Entity Image Renderer
    const entity_image_renderer = new EntityImageRenderer(entity_asset_loader);

    // View
    return new QuestEditorView(
        gui_store,
        quest_editor_store,
        quest_editor_ui_persister,
        new QuestEditorToolBar(
            new QuestEditorToolBarController(gui_store, area_store, quest_editor_store),
        ),
        () => new QuestInfoView(new QuestInfoController(quest_editor_store)),
        () => new NpcCountsView(quest_editor_store),
        () =>
            new QuestEditorRendererView(
                gui_store,
                quest_editor_store,
                area_asset_loader,
                entity_asset_loader,
                create_three_renderer(),
            ),
        () => new AsmEditorView(gui_store, quest_editor_store.quest_runner, asm_editor_store),
        () => new EntityInfoView(quest_editor_store),
        () => new NpcListView(quest_editor_store, entity_image_renderer),
        () => new ObjectListView(quest_editor_store, entity_image_renderer),
        () => new EventsView(quest_editor_store),
        () =>
            new QuestRunnerRendererView(
                gui_store,
                quest_editor_store,
                area_asset_loader,
                entity_asset_loader,
                create_three_renderer(),
            ),
        () => new RegistersView(quest_editor_store.quest_runner),
    );
}
