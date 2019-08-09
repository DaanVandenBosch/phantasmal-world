import { ObservableArea, ObservableAreaVariant, Section } from "../domain";
import { load_area_sections } from "../loading/areas";
import { Episode, EPISODES } from "../data_formats/parsing/quest/Episode";
import { get_areas_for_episode } from "../data_formats/parsing/quest/areas";

class AreaStore {
    private readonly areas: ObservableArea[][] = [];

    constructor() {
        for (const episode of EPISODES) {
            this.areas[episode] = get_areas_for_episode(episode).map(area => {
                const observable_area = new ObservableArea(area.id, area.name, area.order, []);

                for (const variant of area.area_variants) {
                    observable_area.area_variants.push(
                        new ObservableAreaVariant(variant.id, observable_area),
                    );
                }

                return observable_area;
            });
        }
    }

    get_areas_for_episode = (episode: Episode): ObservableArea[] => {
        return this.areas[episode];
    };

    get_area = (episode: Episode, area_id: number): ObservableArea => {
        const area = this.areas[episode].find(a => a.id === area_id);
        if (!area) throw new Error(`Area id ${area_id} for episode ${episode} is invalid.`);
        return area;
    };

    get_variant = (
        episode: Episode,
        area_id: number,
        variant_id: number,
    ): ObservableAreaVariant => {
        const area = this.get_area(episode, area_id);

        const area_variant = area.area_variants[variant_id];
        if (!area_variant)
            throw new Error(
                `Area variant id ${variant_id} for area ${area_id} of episode ${episode} is invalid.`,
            );

        return area_variant;
    };

    get_area_sections = (
        episode: Episode,
        area_id: number,
        variant_id: number,
    ): Promise<Section[]> => {
        return load_area_sections(episode, area_id, variant_id);
    };
}

export const area_store = new AreaStore();
