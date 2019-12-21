/**
 * @jest-environment jsdom
 */

import Logger from "js-logger";
import { IContext } from "js-logger/src/types";
import { initialize } from "./initialize";
import { StubHttpClient } from "./core/HttpClient";
import { DisposableThreeRenderer } from "./core/rendering/Renderer";

test("Initialization and shutdown should succeed without throwing or logging errors.", () => {
    const logged_errors: string[] = [];

    Logger.setHandler((messages: any[], context: IContext) => {
        if (context.level.value >= Logger.ERROR.value) {
            logged_errors.push(Array.prototype.join.call(messages, " "));
        }
    });

    const app = initialize(new StubHttpClient(), () => new StubRenderer());

    expect(app).toBeDefined();
    expect(logged_errors).toEqual([]);

    app.dispose();

    expect(logged_errors).toEqual([]);
});

class StubRenderer implements DisposableThreeRenderer {
    domElement: HTMLCanvasElement = document.createElement("canvas");

    dispose(): void {} // eslint-disable-line

    render(): void {} // eslint-disable-line

    setSize(): void {} // eslint-disable-line
}
