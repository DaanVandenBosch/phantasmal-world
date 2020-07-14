// Import polyfills before anything else.
import "core-js/stable";

import "./core/gui/index.css";
import "@fortawesome/fontawesome-free/js/fontawesome";
import "@fortawesome/fontawesome-free/js/solid";
import "@fortawesome/fontawesome-free/js/regular";
import "@fortawesome/fontawesome-free/js/brands";
import { initialize_application } from "./application";
import { FetchClient } from "./core/HttpClient";
import { WebGLRenderer } from "three";
import { DisposableThreeRenderer } from "./core/rendering/Renderer";
import { Random } from "./core/Random";
import { DateClock } from "./core/Clock";

function create_three_renderer(): DisposableThreeRenderer {
    const renderer = new WebGLRenderer({ antialias: true, alpha: true });
    renderer.setPixelRatio(window.devicePixelRatio);
    return renderer;
}

initialize_application(new FetchClient(), new Random(), new DateClock(), create_three_renderer);
