import { Persister } from "./Persister";
import { throttle } from "lodash";

const LAYOUT_CONFIG_KEY = "QuestEditorUiPersister.layout_config";

class QuestEditorUiPersister extends Persister {
    persist_layout_config = throttle(
        (config: any) => {
            this.persist(LAYOUT_CONFIG_KEY, config);
        },
        1000,
        { leading: false, trailing: true }
    );

    async load_layout_config(components: string[], default_config: any): Promise<any> {
        const config = await this.load(LAYOUT_CONFIG_KEY);

        let valid = this.verify_layout_config(config, new Set(components));

        if (valid) {
            return config;
        } else {
            return default_config;
        }
    }

    private verify_layout_config(
        config: any,
        components: Set<string>,
        found: Set<string> = new Set(),
        first: boolean = true
    ): boolean {
        if (!config) {
            return false;
        }

        if (config.component) {
            if (!components.has(config.component)) {
                return false;
            } else {
                found.add(config.component);
            }
        }

        if (config.content) {
            for (const child of config.content) {
                if (!this.verify_layout_config(child, components, found, false)) {
                    return false;
                }
            }
        }

        return first ? components.size === found.size : true;
    }
}

export const quest_editor_ui_persister = new QuestEditorUiPersister();
