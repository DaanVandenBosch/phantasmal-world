import { Disposable } from "../../core/observable/Disposable";
import { LoadingCache } from "./LoadingCache";
import { HttpClient } from "../../core/HttpClient";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { DisposablePromise } from "../../core/DisposablePromise";
import { parse_qst_to_quest } from "../../core/data_formats/parsing/quest";
import { ArrayBufferCursor } from "../../core/data_formats/block/cursor/ArrayBufferCursor";
import { Endianness } from "../../core/data_formats/block/Endianness";
import { assert } from "../../core/util";
import { Quest } from "../../core/data_formats/parsing/quest/Quest";
import { unwrap } from "../../core/Result";

export class QuestLoader implements Disposable {
    private readonly cache = new LoadingCache<string, ArrayBuffer>();

    constructor(private readonly http_client: HttpClient) {}

    load_default_quest(episode: Episode): DisposablePromise<Quest> {
        if (episode === Episode.II) throw new Error("Episode II not yet supported.");
        if (episode === Episode.IV) throw new Error("Episode IV not yet supported.");

        return this.load_quest("/defaults/default_ep_1.qst");
    }

    dispose(): void {
        this.cache.dispose();
    }

    private load_quest(path: string): DisposablePromise<Quest> {
        return this.cache
            .get_or_set(path, () => this.http_client.get(`/quests${path}`).array_buffer())
            .then(buffer => {
                const result = unwrap(
                    parse_qst_to_quest(new ArrayBufferCursor(buffer, Endianness.Little)),
                );
                assert(result, () => `Quest "${path}" can't be parsed.`);
                return result.quest;
            });
    }
}
