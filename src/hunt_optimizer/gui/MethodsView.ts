import { TabContainer } from "../../core/gui/TabContainer";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { MethodsForEpisodeView } from "./MethodsForEpisodeView";
import { ServerMap } from "../../core/stores/ServerMap";
import { HuntMethodStore } from "../stores/HuntMethodStore";
import { GuiStore } from "../../core/stores/GuiStore";

export class MethodsView extends TabContainer {
    constructor(gui_store: GuiStore, hunt_method_stores: ServerMap<HuntMethodStore>) {
        super(gui_store, {
            class: "hunt_optimizer_MethodsView",
            tabs: [
                {
                    title: "Episode I",
                    key: "episode_1",
                    path: "/methods/episode_1",
                    create_view: async () => {
                        return new MethodsForEpisodeView(hunt_method_stores, Episode.I);
                    },
                },
                {
                    title: "Episode II",
                    key: "episode_2",
                    path: "/methods/episode_2",
                    create_view: async () => {
                        return new MethodsForEpisodeView(hunt_method_stores, Episode.II);
                    },
                },
                {
                    title: "Episode IV",
                    key: "episode_4",
                    path: "/methods/episode_4",
                    create_view: async () => {
                        return new MethodsForEpisodeView(hunt_method_stores, Episode.IV);
                    },
                },
            ],
        });

        this.finalize_construction();
    }
}
