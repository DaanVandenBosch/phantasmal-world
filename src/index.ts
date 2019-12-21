import "./core/gui/index.css";
import Logger from "js-logger";
import "@fortawesome/fontawesome-free/js/fontawesome";
import "@fortawesome/fontawesome-free/js/solid";
import "@fortawesome/fontawesome-free/js/regular";
import "@fortawesome/fontawesome-free/js/brands";
import { initialize } from "./initialize";
import { FetchClient } from "./core/HttpClient";
import { WebGLRenderer } from "three";
import { DisposableThreeRenderer } from "./core/rendering/Renderer";

Logger.useDefaults({
    defaultLevel: (Logger as any)[process.env["LOG_LEVEL"] ?? "OFF"],
});

function create_three_renderer(): DisposableThreeRenderer {
    const renderer = new WebGLRenderer({ antialias: true, alpha: true });
    renderer.setPixelRatio(window.devicePixelRatio);
    return renderer;
}

initialize(new FetchClient(), create_three_renderer);
