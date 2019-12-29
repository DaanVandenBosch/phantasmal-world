import { EntityInfoController } from "./EntityInfoController";
import {
    create_area_store,
    create_quest_editor_store,
} from "../../../test/src/quest_editor/stores/store_creation";
import { with_disposer } from "../../../test/src/core/observables/disposable_helpers";
import { create_new_quest } from "../stores/quest_creation";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { Vector3 } from "three";
import { euler } from "../model/euler";
import { deg_to_rad } from "../../core/math";

test("When input values change, this should be reflected in the selected entity.", () =>
    with_disposer(disposer => {
        const area_store = create_area_store(disposer);
        const store = create_quest_editor_store(disposer, area_store);
        const ctrl = new EntityInfoController(store);

        const quest = create_new_quest(area_store, Episode.I);
        const entity = quest.objects.get(0);
        entity.set_position(new Vector3(0, 0, 0));
        entity.set_rotation(euler(0, 0, 0));
        store.set_current_quest(quest);
        store.set_selected_entity(entity);

        ctrl.set_pos_x(9834.834);
        expect(entity.position.val).toEqual(new Vector3(9834.834, 0, 0));

        ctrl.set_pos_y(84093.87);
        expect(entity.position.val).toEqual(new Vector3(9834.834, 84093.87, 0));

        ctrl.set_pos_z(4279);
        expect(entity.position.val).toEqual(new Vector3(9834.834, 84093.87, 4279));

        ctrl.set_rot_x(180);
        expect(entity.rotation.val.x).toBeCloseTo(deg_to_rad(180), 5);
        expect(entity.rotation.val.y).toBeCloseTo(deg_to_rad(0), 5);
        expect(entity.rotation.val.z).toBeCloseTo(deg_to_rad(0), 5);

        ctrl.set_rot_y(45);
        expect(entity.rotation.val.x).toBeCloseTo(deg_to_rad(180), 5);
        expect(entity.rotation.val.y).toBeCloseTo(deg_to_rad(45), 5);
        expect(entity.rotation.val.z).toBeCloseTo(deg_to_rad(0), 5);

        ctrl.set_rot_z(223.83);
        expect(entity.rotation.val.x).toBeCloseTo(deg_to_rad(180), 5);
        expect(entity.rotation.val.y).toBeCloseTo(deg_to_rad(45), 5);
        expect(entity.rotation.val.z).toBeCloseTo(deg_to_rad(223.83), 5);
    }));
