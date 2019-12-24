import { Persister } from "../../core/Persister";
import { throttle } from "lodash";
import { ComponentConfig, ItemConfigType } from "golden-layout";

const LAYOUT_CONFIG_KEY = "QuestEditorUiPersister.layout_config";

export class QuestEditorUiPersister extends Persister {
    persist_layout_config = throttle(
        (config: ItemConfigType[]) => {
            this.persist(LAYOUT_CONFIG_KEY, this.to_persisted_item_config(config));
        },
        500,
        { leading: false, trailing: true },
    );

    async load_layout_config(
        default_config: ItemConfigType[],
    ): Promise<ItemConfigType[] | undefined> {
        const config = await this.load<ItemConfigType[]>(LAYOUT_CONFIG_KEY);

        if (config) {
            const components = this.extract_components(default_config);
            const verified_config = this.sanitized_layout_config(
                this.from_persisted_item_config(config),
                components,
            );

            if (verified_config) {
                return verified_config;
            }
        }

        return undefined;
    }

    private sanitized_layout_config(
        config: ItemConfigType[],
        components: Map<string, ComponentConfig>,
    ): ItemConfigType[] | undefined {
        const found = new Set<string>();

        const sanitized_config = config.map(child =>
            this.sanitize_layout_child(child, components, found),
        );

        if (found.size !== components.size) {
            // A component was added, use default layout instead of persisted layout.
            return undefined;
        }

        // Filter out removed components.
        return sanitized_config.filter(item => item) as ItemConfigType[];
    }

    /**
     * Removed old components and adds titles and ids to current components.
     * Modifies the given ItemConfigType object.
     */
    private sanitize_layout_child(
        config: ItemConfigType,
        components: Map<string, ComponentConfig>,
        found: Set<string>,
    ): ItemConfigType | undefined {
        if (!config) {
            return undefined;
        }

        switch (config.type) {
            case "component":
                {
                    // Remove corrupted components.
                    if (!("componentName" in config)) {
                        return undefined;
                    }

                    const component = components.get(config.componentName);

                    // Remove deprecated components.
                    if (!component) {
                        return undefined;
                    }

                    found.add(config.componentName);
                    config.id = component.id;
                    config.title = component.title;
                }
                break;

            case "stack":
                // Remove empty stacks.
                if (config.content == undefined || config.content.length === 0) {
                    return undefined;
                }
                break;
        }

        // Sanitize child items.
        if (config.content) {
            config.content = config.content
                .map(child => this.sanitize_layout_child(child, components, found))
                .filter(item => item) as ItemConfigType[];
        }

        // Remove corrupted activeItemIndex properties.
        const cfg = config as any;

        if (
            cfg.activeItemIndex != undefined &&
            (cfg.content == undefined || cfg.activeItemIndex >= cfg.content.length)
        ) {
            cfg.activeItemIndex = undefined;
        }

        return config;
    }

    private extract_components(
        config: ItemConfigType[],
        map: Map<string, ComponentConfig> = new Map(),
    ): Map<string, ComponentConfig> {
        for (const child of config) {
            if ("componentName" in child) {
                map.set(child.componentName, child);
            }

            if (child.content) {
                this.extract_components(child.content, map);
            }
        }

        return map;
    }

    private to_persisted_item_config(config: ItemConfigType[]): PersistedItemConfig[] {
        return config.map(item => ({
            id: item.id,
            type: item.type,
            componentName: (item as any).componentName,
            width: item.width,
            height: item.height,
            activeItemIndex: (item as any).activeItemIndex,
            content: item.content && this.to_persisted_item_config(item.content),
        }));
    }

    /**
     * This simply makes a copy to ensure legacy properties are removed.
     */
    private from_persisted_item_config(config: PersistedItemConfig[]): ItemConfigType[] {
        return config.map(item => ({
            id: item.id,
            type: item.type,
            componentName: item.componentName,
            width: item.width,
            height: item.height,
            activeItemIndex: item.activeItemIndex,
            content: item.content && this.from_persisted_item_config(item.content),
            isClosable: item.type === "component" ? false : undefined,
        }));
    }
}

type PersistedItemConfig = {
    id?: string | string[];
    type: string;
    componentName?: string;
    width?: number;
    height?: number;
    /**
     * Used by stacks.
     */
    activeItemIndex?: number;
    content?: PersistedItemConfig[];
};
