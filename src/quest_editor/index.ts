import { QuestEditorView } from "./gui/QuestEditorView";
import { GuiStore } from "../core/stores/GuiStore";
import { QuestEditorStore } from "./stores/QuestEditorStore";
import { AsmEditorStore } from "./stores/AsmEditorStore";
import { AreaStore } from "./stores/AreaStore";
import { AreaAssetLoader } from "./loading/AreaAssetLoader";
import { HttpClient } from "../core/HttpClient";
import { EntityImageRenderer } from "./rendering/EntityImageRenderer";
import { EntityAssetLoader } from "./loading/EntityAssetLoader";

export function initialize_quest_editor(
    http_client: HttpClient,
    gui_store: GuiStore,
): QuestEditorView {
    // Asset Loaders
    const area_asset_loader = new AreaAssetLoader(http_client);
    const entity_asset_loader = new EntityAssetLoader(http_client);

    // Stores
    const area_store = new AreaStore(area_asset_loader);
    const quest_editor_store = new QuestEditorStore(gui_store, area_store);
    const asm_editor_store = new AsmEditorStore(quest_editor_store);

    // Entity Image Renderer
    const entity_image_renderer = new EntityImageRenderer(entity_asset_loader);

    // View
    return new QuestEditorView(
        gui_store,
        area_store,
        quest_editor_store,
        asm_editor_store,
        area_asset_loader,
        entity_asset_loader,
        entity_image_renderer,
    );
}
