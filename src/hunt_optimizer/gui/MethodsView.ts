import { TabContainer } from "../../core/gui/TabContainer";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { MethodsForEpisodeView } from "./MethodsForEpisodeView";
import { ServerMap } from "../../core/stores/ServerMap";
import { HuntMethodStore } from "../stores/HuntMethodStore";

export class MethodsView extends TabContainer {
    constructor(hunt_method_stores: ServerMap<HuntMethodStore>) {
        super({
            class: "hunt_optimizer_MethodsView",
            tabs: [
                {
                    title: "Episode I",
                    key: "episode_1",
                    create_view: async function() {
                        return new MethodsForEpisodeView(hunt_method_stores, Episode.I);
                    },
                },
                {
                    title: "Episode II",
                    key: "episode_2",
                    create_view: async function() {
                        return new MethodsForEpisodeView(hunt_method_stores, Episode.II);
                    },
                },
                {
                    title: "Episode IV",
                    key: "episode_4",
                    create_view: async function() {
                        return new MethodsForEpisodeView(hunt_method_stores, Episode.IV);
                    },
                },
            ],
        });

        this.finalize_construction();
    }
}
