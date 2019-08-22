import { Persister } from "../../core/persistence";
import { throttle } from "lodash";
import GoldenLayout from "golden-layout";

const LAYOUT_CONFIG_KEY = "QuestEditorUiPersister.layout_config";

export class QuestEditorUiPersister extends Persister {
    persist_layout_config = throttle(
        (config: any) => {
            this.persist(LAYOUT_CONFIG_KEY, config);
        },
        500,
        { leading: false, trailing: true },
    );

    async load_layout_config(
        components: string[],
        default_config: GoldenLayout.ItemConfigType[],
    ): Promise<any> {
        const config = await this.load<GoldenLayout.ItemConfigType[]>(LAYOUT_CONFIG_KEY);

        if (config && this.verify_layout_config(config, components)) {
            return config;
        } else {
            return default_config;
        }
    }

    private verify_layout_config(
        config: GoldenLayout.ItemConfigType[],
        components: string[],
    ): boolean {
        const set = new Set(components);

        for (const child of config) {
            if (!this.verify_layout_child(child, set, new Set(), true)) {
                return false;
            }
        }

        return true;
    }

    private verify_layout_child(
        config: GoldenLayout.ItemConfigType,
        components: Set<string>,
        found: Set<string>,
        first: boolean,
    ): boolean {
        if (!config) {
            return false;
        }

        if ("componentName" in config) {
            if (!components.has(config.componentName)) {
                return false;
            } else {
                found.add(config.componentName);
            }
        }

        if (config.content) {
            for (const child of config.content) {
                if (!this.verify_layout_child(child, components, found, false)) {
                    return false;
                }
            }
        }

        return first ? components.size === found.size : true;
    }
}

export const quest_editor_ui_persister = new QuestEditorUiPersister();
