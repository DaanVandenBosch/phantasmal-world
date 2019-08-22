import { property } from "../../core/observable";
import { WritableProperty } from "../../core/observable/WritableProperty";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";

export class ObservableQuest {
    readonly id: WritableProperty<number>;
    readonly language: WritableProperty<number>;
    readonly name: WritableProperty<string>;
    readonly short_description: WritableProperty<string>;
    readonly long_description: WritableProperty<string>;
    readonly episode: Episode;

    constructor(
        id: number,
        language: number,
        name: string,
        short_description: string,
        long_description: string,
        episode: Episode,
    ) {
        this.id = property(id);
        this.language = property(language);
        this.name = property(name);
        this.short_description = property(short_description);
        this.long_description = property(long_description);
        this.episode = episode;
    }
}
