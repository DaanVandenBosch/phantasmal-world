import { action, computed, observable } from "mobx";
import { check_episode, Episode } from "../data_formats/parsing/quest/Episode";
import { ObservableAreaVariant } from "./ObservableAreaVariant";
import { area_store } from "../stores/AreaStore";
import { DatUnknown } from "../data_formats/parsing/quest/dat";
import { Segment } from "../scripting/instructions";
import { ObservableQuestNpc, ObservableQuestObject } from "./index";
import Logger from "js-logger";

const logger = Logger.get("domain/ObservableQuest");

export class ObservableQuest {
    @observable private _id!: number;

    get id(): number {
        return this._id;
    }

    @action
    set_id(id: number): void {
        if (!Number.isInteger(id) || id < 0 || id > 4294967295)
            throw new Error("id must be an integer greater than 0 and less than 4294967295.");
        this._id = id;
    }

    @observable private _language!: number;

    get language(): number {
        return this._language;
    }

    @action
    set_language(language: number): void {
        if (!Number.isInteger(language)) throw new Error("language must be an integer.");
        this._language = language;
    }

    @observable private _name!: string;

    get name(): string {
        return this._name;
    }

    @action
    set_name(name: string): void {
        if (name.length > 32) throw new Error("name can't be longer than 32 characters.");
        this._name = name;
    }

    @observable private _short_description!: string;

    get short_description(): string {
        return this._short_description;
    }

    @action
    set_short_description(short_description: string): void {
        if (short_description.length > 128)
            throw new Error("short_description can't be longer than 128 characters.");
        this._short_description = short_description;
    }

    @observable private _long_description!: string;

    get long_description(): string {
        return this._long_description;
    }

    @action
    set_long_description(long_description: string): void {
        if (long_description.length > 288)
            throw new Error("long_description can't be longer than 288 characters.");
        this._long_description = long_description;
    }

    readonly episode: Episode;

    @observable readonly objects: ObservableQuestObject[];
    @observable readonly npcs: ObservableQuestNpc[];

    /**
     * Map of area IDs to entity counts.
     */
    @computed get entities_per_area(): Map<number, number> {
        const map = new Map<number, number>();

        for (const npc of this.npcs) {
            map.set(npc.area_id, (map.get(npc.area_id) || 0) + 1);
        }

        for (const obj of this.objects) {
            map.set(obj.area_id, (map.get(obj.area_id) || 0) + 1);
        }

        return map;
    }

    @observable.ref private _map_designations!: Map<number, number>;

    /**
     * Map of area IDs to area variant IDs. One designation per area.
     */
    get map_designations(): Map<number, number> {
        return this._map_designations;
    }

    set_map_designations(map_designations: Map<number, number>): void {
        this._map_designations = map_designations;
    }

    /**
     * One variant per area.
     */
    @computed get area_variants(): ObservableAreaVariant[] {
        const variants = new Map<number, ObservableAreaVariant>();

        for (const area_id of this.entities_per_area.keys()) {
            try {
                variants.set(area_id, area_store.get_variant(this.episode, area_id, 0));
            } catch (e) {
                logger.warn(e);
            }
        }

        for (const [area_id, variant_id] of this._map_designations) {
            try {
                variants.set(area_id, area_store.get_variant(this.episode, area_id, variant_id));
            } catch (e) {
                logger.warn(e);
            }
        }

        return [...variants.values()];
    }

    /**
     * (Partial) raw DAT data that can't be parsed yet by Phantasmal.
     */
    readonly dat_unknowns: DatUnknown[];
    readonly object_code: Segment[];
    readonly shop_items: number[];

    constructor(
        id: number,
        language: number,
        name: string,
        short_description: string,
        long_description: string,
        episode: Episode,
        map_designations: Map<number, number>,
        objects: ObservableQuestObject[],
        npcs: ObservableQuestNpc[],
        dat_unknowns: DatUnknown[],
        object_code: Segment[],
        shop_items: number[],
    ) {
        check_episode(episode);
        if (!map_designations) throw new Error("map_designations is required.");
        if (!Array.isArray(objects)) throw new Error("objs is required.");
        if (!Array.isArray(npcs)) throw new Error("npcs is required.");
        if (!dat_unknowns) throw new Error("dat_unknowns is required.");
        if (!object_code) throw new Error("object_code is required.");
        if (!shop_items) throw new Error("shop_items is required.");

        this.set_id(id);
        this.set_language(language);
        this.set_name(name);
        this.set_short_description(short_description);
        this.set_long_description(long_description);
        this.episode = episode;
        this.set_map_designations(map_designations);
        this.objects = objects;
        this.npcs = npcs;
        this.dat_unknowns = dat_unknowns;
        this.object_code = object_code;
        this.shop_items = shop_items;
    }
}
