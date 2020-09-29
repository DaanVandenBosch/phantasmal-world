import { QuestNpcModel } from "./QuestNpcModel";
import { NpcType } from "../../core/data_formats/parsing/quest/npc_types";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { Vector3 } from "three";
import { SectionModel } from "./SectionModel";
import { QuestEntityModel } from "./QuestEntityModel";
import { AreaStore } from "../stores/AreaStore";
import { StubHttpClient } from "../../core/HttpClient";
import { AreaAssetLoader } from "../loading/AreaAssetLoader";
import { euler } from "./euler";
import { create_quest_npc } from "../../core/data_formats/parsing/quest/QuestNpc";

const area_store = new AreaStore(new AreaAssetLoader(new StubHttpClient()));

test("After changing section, world position should change accordingly.", () => {
    const entity = create_entity();

    expect(entity.world_position.val).toEqual(entity.position.val);
    expect(entity.world_position.val).toEqual(new Vector3(5, 5, 5));

    // Set initial section.
    entity.set_section(
        new SectionModel(
            20,
            new Vector3(7, 7, 7),
            euler(0, 0, 0),
            area_store.get_variant(Episode.I, 0, 0),
        ),
    );

    expect(entity.world_position.val).toEqual(new Vector3(12, 12, 12));

    // Change to different section.
    entity.set_section(
        new SectionModel(
            30,
            new Vector3(11, 11, 11),
            euler(0, 0, 0),
            area_store.get_variant(Episode.I, 0, 0),
        ),
    );

    expect(entity.world_position.val).toEqual(new Vector3(16, 16, 16));
});

function create_entity(): QuestEntityModel {
    const entity = new QuestNpcModel(
        create_quest_npc(NpcType.AlRappy, Episode.I, area_store.get_area(Episode.I, 0).id, 0),
    );
    entity.set_position(new Vector3(5, 5, 5));
    return entity;
}
