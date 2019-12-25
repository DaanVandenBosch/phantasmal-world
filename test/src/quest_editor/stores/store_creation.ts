import { QuestEditorStore } from "../../../../src/quest_editor/stores/QuestEditorStore";
import { GuiStore } from "../../../../src/core/stores/GuiStore";
import { AreaStore } from "../../../../src/quest_editor/stores/AreaStore";
import { AreaAssetLoader } from "../../../../src/quest_editor/loading/AreaAssetLoader";
import { FileSystemHttpClient } from "../../core/FileSystemHttpClient";
import { Disposer } from "../../../../src/core/observable/Disposer";

export function create_area_store(disposer: Disposer): AreaStore {
    return disposer.add(
        new AreaStore(disposer.add(new AreaAssetLoader(new FileSystemHttpClient()))),
    );
}

export function create_quest_editor_store(
    disposer: Disposer,
    area_store: AreaStore = create_area_store(disposer),
): QuestEditorStore {
    return disposer.add(new QuestEditorStore(disposer.add(new GuiStore()), area_store));
}
