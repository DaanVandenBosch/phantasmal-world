import { QuestNpcModel } from "./QuestNpcModel";
import { npc_data, NpcType } from "../../core/data_formats/parsing/quest/npc_types";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { Euler, Vector3 } from "three";
import { SectionModel } from "./SectionModel";
import { QuestEntityModel } from "./QuestEntityModel";
import { AreaStore } from "../stores/AreaStore";
import { DummyClient } from "../../core/HttpClient";
import { AreaAssetLoader } from "../loading/AreaAssetLoader";

const area_store = new AreaStore(new AreaAssetLoader(new DummyClient()));

test("After changing section, world position should change accordingly.", () => {
    const entity = create_entity();

    expect(entity.world_position.val).toEqual(entity.position.val);
    expect(entity.world_position.val).toEqual(new Vector3(5, 5, 5));

    // Set initial section.
    entity.set_section(
        new SectionModel(
            20,
            new Vector3(7, 7, 7),
            new Euler(0, 0, 0, "ZXY"),
            area_store.get_variant(Episode.I, 0, 0),
        ),
    );

    expect(entity.world_position.val).toEqual(new Vector3(12, 12, 12));

    // Change to different section.
    entity.set_section(
        new SectionModel(
            30,
            new Vector3(11, 11, 11),
            new Euler(0, 0, 0, "ZXY"),
            area_store.get_variant(Episode.I, 0, 0),
        ),
    );

    expect(entity.world_position.val).toEqual(new Vector3(16, 16, 16));
});

function create_entity(): QuestEntityModel {
    return new QuestNpcModel(
        NpcType.AlRappy,
        npc_data(NpcType.AlRappy).pso_type_id!!,
        1,
        0,
        0,
        0,
        0,
        area_store.get_area(Episode.I, 0).id,
        20,
        new Vector3(5, 5, 5),
        new Euler(0, 0, 0, "ZXY"),
        new Vector3(1, 1, 1),
        [Array(10).fill(0xdead), Array(4).fill(0xdead)],
    );
}
