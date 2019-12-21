import { AreaModel } from "../model/AreaModel";
import { AreaVariantModel } from "../model/AreaVariantModel";
import { Episode, EPISODES } from "../../core/data_formats/parsing/quest/Episode";
import { SectionModel } from "../model/SectionModel";
import { get_areas_for_episode } from "../../core/data_formats/parsing/quest/areas";
import { AreaAssetLoader } from "../loading/AreaAssetLoader";

export class AreaStore {
    private readonly areas: AreaModel[][] = [];

    constructor(private readonly area_asset_loader: AreaAssetLoader) {
        for (const episode of EPISODES) {
            this.areas[episode] = get_areas_for_episode(episode).map(area => {
                const observable_area = new AreaModel(area.id, area.name, area.order, []);

                for (const variant of area.area_variants) {
                    observable_area.area_variants.push(
                        new AreaVariantModel(variant.id, observable_area),
                    );
                }

                return observable_area;
            });
        }
    }

    get_areas_for_episode = (episode: Episode): AreaModel[] => {
        return this.areas[episode];
    };

    get_area = (episode: Episode, area_id: number): AreaModel => {
        const area = this.areas[episode].find(a => a.id === area_id);
        if (!area) throw new Error(`Area id ${area_id} for episode ${episode} is invalid.`);
        return area;
    };

    get_variant = (episode: Episode, area_id: number, variant_id: number): AreaVariantModel => {
        const area = this.get_area(episode, area_id);

        const area_variant = area.area_variants[variant_id];
        if (!area_variant)
            throw new Error(
                `Area variant id ${variant_id} for area ${area_id} of episode ${episode} is invalid.`,
            );

        return area_variant;
    };

    get_area_sections = (episode: Episode, variant: AreaVariantModel): Promise<SectionModel[]> => {
        return this.area_asset_loader.load_sections(episode, variant);
    };
}
