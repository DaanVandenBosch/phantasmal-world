import { TabContainer } from "../../core/gui/TabContainer";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { MethodsForEpisodeView } from "./MethodsForEpisodeView";

export class MethodsView extends TabContainer {
    constructor() {
        super({
            class: "hunt_optimizer_MethodsView",
            tabs: [
                {
                    title: "Episode I",
                    key: "episode_1",
                    create_view: async function() {
                        return new MethodsForEpisodeView(Episode.I);
                    },
                },
                {
                    title: "Episode II",
                    key: "episode_2",
                    create_view: async function() {
                        return new MethodsForEpisodeView(Episode.II);
                    },
                },
                {
                    title: "Episode IV",
                    key: "episode_4",
                    create_view: async function() {
                        return new MethodsForEpisodeView(Episode.IV);
                    },
                },
            ],
        });

        this.finalize_construction(MethodsView.prototype);
    }
}
