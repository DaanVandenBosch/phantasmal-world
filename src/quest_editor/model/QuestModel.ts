import { list_property, map, property } from "../../core/observable";
import { WritableProperty } from "../../core/observable/property/WritableProperty";
import { check_episode, Episode } from "../../core/data_formats/parsing/quest/Episode";
import { QuestObjectModel } from "./QuestObjectModel";
import { QuestNpcModel } from "./QuestNpcModel";
import { DatUnknown } from "../../core/data_formats/parsing/quest/dat";
import { Segment } from "../scripting/instructions";
import { Property } from "../../core/observable/property/Property";
import Logger from "js-logger";
import { AreaVariantModel } from "./AreaVariantModel";
import { area_store } from "../stores/AreaStore";
import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { WritableListProperty } from "../../core/observable/property/list/WritableListProperty";

const logger = Logger.get("quest_editor/model/QuestModel");

export class QuestModel {
    readonly id: Property<number>;

    set_id(id: number): this {
        if (id < 0) throw new Error(`id should be greater than or equal to 0, was ${id}.`);

        this._id.val = id;
        return this;
    }

    readonly language: Property<number>;

    set_language(language: number): this {
        if (language < 0)
            throw new Error(`language should be greater than or equal to 0, was ${language}.`);

        this._language.val = language;
        return this;
    }

    readonly name: Property<string>;

    set_name(name: string): this {
        if (name.length > 32)
            throw new Error(`name can't be longer than 32 characters, got "${name}".`);

        this._name.val = name;
        return this;
    }

    readonly short_description: Property<string>;

    set_short_description(short_description: string): this {
        if (short_description.length > 128)
            throw new Error(
                `short_description can't be longer than 128 characters, got "${short_description}".`,
            );

        this._short_description.val = short_description;
        return this;
    }

    readonly long_description: Property<string>;

    set_long_description(long_description: string): this {
        if (long_description.length > 288)
            throw new Error(
                `long_description can't be longer than 288 characters, got "${long_description}".`,
            );

        this._long_description.val = long_description;
        return this;
    }

    readonly episode: Episode;

    /**
     * Map of area IDs to entity counts.
     */
    readonly entities_per_area: Property<Map<number, number>>;

    /**
     * Map of area IDs to area variant IDs. One designation per area.
     */
    readonly map_designations: Property<Map<number, number>>;

    set_map_designations(map_designations: Map<number, number>): this {
        this._map_designations.val = map_designations;
        return this;
    }

    /**
     * One variant per area.
     */
    readonly area_variants: ListProperty<AreaVariantModel>;

    readonly objects: ListProperty<QuestObjectModel>;

    readonly npcs: ListProperty<QuestNpcModel>;

    /**
     * (Partial) raw DAT data that can't be parsed yet by Phantasmal.
     */
    readonly dat_unknowns: DatUnknown[];

    readonly object_code: Segment[];

    readonly shop_items: number[];

    private readonly _id: WritableProperty<number> = property(0);
    private readonly _language: WritableProperty<number> = property(0);
    private readonly _name: WritableProperty<string> = property("");
    private readonly _short_description: WritableProperty<string> = property("");
    private readonly _long_description: WritableProperty<string> = property("");
    private readonly _map_designations: WritableProperty<Map<number, number>>;
    private readonly _area_variants: WritableListProperty<AreaVariantModel> = list_property();

    constructor(
        id: number,
        language: number,
        name: string,
        short_description: string,
        long_description: string,
        episode: Episode,
        map_designations: Map<number, number>,
        objects: QuestObjectModel[],
        npcs: QuestNpcModel[],
        dat_unknowns: DatUnknown[],
        object_code: Segment[],
        shop_items: number[],
    ) {
        check_episode(episode);
        if (!map_designations) throw new Error("map_designations is required.");
        if (!Array.isArray(objects)) throw new Error("objs is required.");
        if (!Array.isArray(npcs)) throw new Error("npcs is required.");
        if (!Array.isArray(dat_unknowns)) throw new Error("dat_unknowns is required.");
        if (!Array.isArray(object_code)) throw new Error("object_code is required.");
        if (!Array.isArray(shop_items)) throw new Error("shop_items is required.");

        this.id = this._id;
        this.language = this._language;
        this.name = this._name;
        this.short_description = this._short_description;
        this.long_description = this._long_description;
        this.area_variants = this._area_variants;

        this.set_id(id);
        this.set_language(language);
        this.set_name(name);
        this.set_short_description(short_description);
        this.set_long_description(long_description);
        this.episode = episode;
        this._map_designations = property(map_designations);
        this.map_designations = this._map_designations;
        this.objects = list_property(undefined, ...objects);
        this.npcs = list_property(undefined, ...npcs);
        this.dat_unknowns = dat_unknowns;
        this.object_code = object_code;
        this.shop_items = shop_items;

        this.entities_per_area = map(
            (npcs, objects) => {
                const map = new Map<number, number>();

                for (const npc of npcs) {
                    map.set(npc.area_id, (map.get(npc.area_id) || 0) + 1);
                }

                for (const obj of objects) {
                    map.set(obj.area_id, (map.get(obj.area_id) || 0) + 1);
                }

                return map;
            },
            this.npcs,
            this.objects,
        );

        this.entities_per_area.observe(this.update_area_variants);
        this.map_designations.observe(this.update_area_variants);
    }

    private update_area_variants = (): void => {
        const variants = new Map<number, AreaVariantModel>();

        for (const area_id of this.entities_per_area.val.keys()) {
            try {
                variants.set(area_id, area_store.get_variant(this.episode, area_id, 0));
            } catch (e) {
                logger.warn(e);
            }
        }

        for (const [area_id, variant_id] of this.map_designations.val) {
            try {
                variants.set(area_id, area_store.get_variant(this.episode, area_id, variant_id));
            } catch (e) {
                logger.warn(e);
            }
        }

        this._area_variants.val = [...variants.values()];
    };
}