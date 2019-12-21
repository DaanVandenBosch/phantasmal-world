import "./core/gui/index.css";
import Logger from "js-logger";
import "@fortawesome/fontawesome-free/js/fontawesome";
import "@fortawesome/fontawesome-free/js/solid";
import "@fortawesome/fontawesome-free/js/regular";
import "@fortawesome/fontawesome-free/js/brands";
import { initialize } from "./initialize";
import { FetchClient } from "./core/HttpClient";

Logger.useDefaults({
    defaultLevel: (Logger as any)[process.env["LOG_LEVEL"] ?? "OFF"],
});

initialize(new FetchClient());
