import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { NpcType } from "../../core/data_formats/parsing/quest/npc_types";
import { SimpleQuestModel } from "./SimpleQuestModel";
import { Property } from "../../core/observable/property/Property";
import { WritableProperty } from "../../core/observable/property/WritableProperty";
import { property } from "../../core/observable";
import { Duration } from "luxon";

export class HuntMethodModel {
    readonly id: string;
    readonly name: string;
    readonly episode: Episode;
    readonly quest: SimpleQuestModel;
    readonly enemy_counts: Map<NpcType, number>;
    /**
     * The time it takes to complete the quest in hours.
     */
    readonly default_time: Duration;
    /**
     * The time it takes to complete the quest in hours as specified by the user.
     */
    readonly user_time: Property<Duration | undefined>;
    readonly time: Property<Duration>;

    private readonly _user_time: WritableProperty<Duration | undefined>;

    constructor(id: string, name: string, quest: SimpleQuestModel, default_time: Duration) {
        if (!id) throw new Error("id is required.");
        if (!Duration.isDuration(default_time))
            throw new Error("default_time must a valid duration.");
        if (!name) throw new Error("name is required.");
        if (!quest) throw new Error("quest is required.");

        this.id = id;
        this.name = name;
        this.episode = quest.episode;
        this.quest = quest;
        this.enemy_counts = quest.enemy_counts;
        this.default_time = default_time;

        this._user_time = property(undefined);
        this.user_time = this._user_time;

        this.time = this.user_time.map(user_time =>
            user_time != undefined ? user_time : this.default_time,
        );
    }

    set_user_time(user_time?: Duration): this {
        this._user_time.val = user_time;
        return this;
    }
}
