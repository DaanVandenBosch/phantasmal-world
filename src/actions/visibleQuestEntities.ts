import { action } from "mobx";
import { VisibleQuestEntity, Vec3, Section } from "../domain";

export const setPositionOnVisibleQuestEntity = action('setPositionOnVisibleQuestEntity',
    (entity: VisibleQuestEntity, position: Vec3, section?: Section) => {
        entity.position = position;

        if (section) {
            entity.section = section;
        }
    }
);