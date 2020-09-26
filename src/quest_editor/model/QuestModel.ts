import { list_property, map, property } from "../../core/observable";
import { WritableProperty } from "../../core/observable/property/WritableProperty";
import { check_episode, Episode } from "../../core/data_formats/parsing/quest/Episode";
import { QuestObjectModel } from "./QuestObjectModel";
import { QuestNpcModel } from "./QuestNpcModel";
import { DatUnknown } from "../../core/data_formats/parsing/quest/dat";
import { Segment } from "../../core/data_formats/asm/instructions";
import { Property } from "../../core/observable/property/Property";
import { AreaVariantModel } from "./AreaVariantModel";
import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { WritableListProperty } from "../../core/observable/property/list/WritableListProperty";
import { QuestEntityModel } from "./QuestEntityModel";
import { entity_type_to_string } from "../../core/data_formats/parsing/quest/Quest";
import { QuestEventDagModel } from "./QuestEventDagModel";
import { assert, defined, require_array } from "../../core/util";
import { AreaStore } from "../stores/AreaStore";
import { LogManager } from "../../core/logging";
import { QuestEventModel } from "./QuestEventModel";

const logger = LogManager.get("quest_editor/model/QuestModel");

export class QuestModel {
    private readonly _id: WritableProperty<number> = property(0);
    private readonly _language: WritableProperty<number> = property(0);
    private readonly _name: WritableProperty<string> = property("");
    private readonly _short_description: WritableProperty<string> = property("");
    private readonly _long_description: WritableProperty<string> = property("");
    private readonly _map_designations: WritableProperty<Map<number, number>>;
    private readonly _area_variants: WritableListProperty<AreaVariantModel> = list_property();
    private readonly _objects: WritableListProperty<QuestObjectModel>;
    private readonly _npcs: WritableListProperty<QuestNpcModel>;

    readonly id: Property<number> = this._id;

    readonly language: Property<number> = this._language;

    readonly name: Property<string> = this._name;

    readonly short_description: Property<string> = this._short_description;

    readonly long_description: Property<string> = this._long_description;

    readonly episode: Episode;

    /**
     * Map of area IDs to entity counts.
     */
    readonly entities_per_area: Property<Map<number, number>>;

    /**
     * Map of area IDs to area variant IDs. One designation per area.
     */
    readonly map_designations: Property<Map<number, number>>;

    /**
     * One variant per area.
     */
    readonly area_variants: ListProperty<AreaVariantModel> = this._area_variants;

    readonly objects: ListProperty<QuestObjectModel>;

    readonly npcs: ListProperty<QuestNpcModel>;

    /**
     * Maps area IDs to event DAGs.
     */
    readonly event_dags: Map<number, QuestEventDagModel>;

    /**
     * (Partial) raw DAT data that can't be parsed yet by Phantasmal.
     */
    readonly dat_unknowns: readonly DatUnknown[];

    readonly object_code: Segment[];

    readonly shop_items: readonly number[];

    constructor(
        private readonly area_store: AreaStore,
        id: number,
        language: number,
        name: string,
        short_description: string,
        long_description: string,
        episode: Episode,
        map_designations: Map<number, number>,
        objects: readonly QuestObjectModel[],
        npcs: readonly QuestNpcModel[],
        event_dags: Map<number, QuestEventDagModel>,
        dat_unknowns: readonly DatUnknown[],
        object_code: Segment[],
        shop_items: readonly number[],
    ) {
        check_episode(episode);
        defined(map_designations, "map_designations");
        require_array(objects, "objs");
        require_array(npcs, "npcs");
        defined(event_dags, "event_dags");
        require_array(dat_unknowns, "dat_unknowns");
        require_array(object_code, "object_code");
        require_array(shop_items, "shop_items");

        this.set_id(id);
        this.set_language(language);
        this.set_name(name);
        this.set_short_description(short_description);
        this.set_long_description(long_description);
        this.episode = episode;
        this._map_designations = property(map_designations);
        this.map_designations = this._map_designations;
        this._objects = list_property(undefined, ...objects);
        this.objects = this._objects;
        this._npcs = list_property(undefined, ...npcs);
        this.npcs = this._npcs;
        this.event_dags = event_dags;
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

        this.entities_per_area.observe(this.update_area_variants, { call_now: true });
        this.map_designations.observe(this.update_area_variants, { call_now: true });
    }

    set_id(id: number): this {
        assert(id >= 0, `id should be greater than or equal to 0, was ${id}.`);

        this._id.val = id;
        return this;
    }

    set_language(language: number): this {
        assert(language >= 0, `language should be greater than or equal to 0, was ${language}.`);

        this._language.val = language;
        return this;
    }

    set_name(name: string): this {
        assert(name.length <= 32, `name can't be longer than 32 characters, got "${name}".`);

        this._name.val = name;
        return this;
    }

    set_short_description(short_description: string): this {
        assert(
            short_description.length <= 128,
            `short_description can't be longer than 128 characters, got "${short_description}".`,
        );

        this._short_description.val = short_description;
        return this;
    }

    set_long_description(long_description: string): this {
        assert(
            long_description.length <= 288,
            `long_description can't be longer than 288 characters, got "${long_description}".`,
        );

        this._long_description.val = long_description;
        return this;
    }

    set_map_designations(map_designations: Map<number, number>): this {
        this._map_designations.val = map_designations;
        return this;
    }

    add_entity(entity: QuestEntityModel): void {
        if (entity instanceof QuestObjectModel) {
            this.add_object(entity);
        } else if (entity instanceof QuestNpcModel) {
            this.add_npc(entity);
        } else {
            throw new Error(`${entity_type_to_string(entity.type)} not supported.`);
        }
    }

    add_object(object: QuestObjectModel): void {
        this._objects.push(object);
    }

    add_npc(npc: QuestNpcModel): void {
        this._npcs.push(npc);
    }

    remove_entity(entity: QuestEntityModel): void {
        if (entity instanceof QuestObjectModel) {
            this._objects.remove(entity);
        } else if (entity instanceof QuestNpcModel) {
            this._npcs.remove(entity);
        } else {
            throw new Error(`${entity_type_to_string(entity.type)} not supported.`);
        }
    }

    insert_event(
        index: number,
        event: QuestEventModel,
        parents: readonly QuestEventModel[],
        children: readonly QuestEventModel[],
    ): void {
        const area_id = event.wave.area_id.val;
        let dag = this.event_dags.get(area_id);

        if (!dag) {
            dag = new QuestEventDagModel(area_id);
            this.event_dags.set(area_id, dag);
        }

        dag.insert_event(index, event, parents, children);
    }

    get_event_dag_or_create(area_id: number): QuestEventDagModel {
        let dag = this.event_dags.get(area_id);

        if (!dag) {
            dag = new QuestEventDagModel(area_id);
            this.event_dags.set(area_id, dag);
        }

        return dag;
    }

    add_event(
        event: QuestEventModel,
        parents: readonly QuestEventModel[],
        children: readonly QuestEventModel[],
    ): void {
        const area_id = event.wave.area_id.val;
        let dag = this.event_dags.get(area_id);

        if (!dag) {
            dag = new QuestEventDagModel(area_id);
            this.event_dags.set(area_id, dag);
        }

        dag.add_event(event, parents, children);
    }

    remove_event(event_dag: QuestEventDagModel, event: QuestEventModel): void {
        for (const npc of this.npcs) {
            if (npc.wave.val === event.wave) {
                npc.set_wave(undefined);
            }
        }

        event_dag.remove_event(event);
    }

    private update_area_variants = (): void => {
        const variants = new Map<number, AreaVariantModel>();

        for (const area_id of this.entities_per_area.val.keys()) {
            try {
                variants.set(area_id, this.area_store.get_variant(this.episode, area_id, 0));
            } catch (e) {
                logger.warn(e);
            }
        }

        for (const [area_id, variant_id] of this.map_designations.val) {
            try {
                variants.set(
                    area_id,
                    this.area_store.get_variant(this.episode, area_id, variant_id),
                );
            } catch (e) {
                logger.warn(e);
            }
        }

        this._area_variants.val = [...variants.values()];
    };
}
