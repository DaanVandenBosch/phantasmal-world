/**
 * @jest-environment jsdom
 */

import Logger from "js-logger";
import { IContext } from "js-logger/src/types";
import { initialize } from "./initialize";
import { StubHttpClient } from "./core/HttpClient";
import { DisposableThreeRenderer } from "./core/rendering/Renderer";

for (const path of [undefined, "/viewer", "/quest_editor", "/hunt_optimizer"]) {
    const with_path = path == undefined ? "without specific path" : `with path ${path}`;

    test(`Initialization and shutdown ${with_path} should succeed without throwing errors or logging with level WARN or above.`, () => {
        const logged_errors: string[] = [];

        Logger.setHandler((messages: any[], context: IContext) => {
            if (context.level.value >= Logger.WARN.value) {
                logged_errors.push(Array.prototype.join.call(messages, " "));
            }
        });

        if (path != undefined) {
            window.location.hash = path;
        }

        const app = initialize(new StubHttpClient(), () => new StubRenderer());

        expect(app).toBeDefined();
        expect(logged_errors).toEqual([]);

        app.dispose();

        expect(logged_errors).toEqual([]);
    });
}

class StubRenderer implements DisposableThreeRenderer {
    domElement: HTMLCanvasElement = document.createElement("canvas");

    dispose(): void {} // eslint-disable-line

    render(): void {} // eslint-disable-line

    setSize(): void {} // eslint-disable-line
}
