import React from "react";
import ReactDOM from "react-dom";
import Logger from "js-logger";
import styles from "./core/ui/index.css";
import { ApplicationComponent } from "./application/ui/ApplicationComponent";
// import "react-virtualized/styles.css";
// import "react-select/dist/react-select.css";
// import "react-virtualized-select/styles.css";
import "golden-layout/src/css/goldenlayout-base.css";
import "golden-layout/src/css/goldenlayout-dark-theme.css";
// import "antd/dist/antd.less";
import { initialize } from "./new";

Logger.useDefaults({
    defaultLevel: (Logger as any)[process.env["LOG_LEVEL"] || "OFF"],
});

// Disable native undo/redo.
document.addEventListener("keydown", e => {
    const kbe = e as KeyboardEvent;

    if (kbe.ctrlKey && !kbe.altKey && kbe.key.toUpperCase() === "Z") {
        kbe.preventDefault();
    }
});
// This doesn't work in FireFox:
document.addEventListener("beforeinput", e => {
    const ie = e as any;

    if (ie.inputType === "historyUndo" || ie.inputType === "historyRedo") {
        e.preventDefault();
    }
});

// const root_element = document.createElement("div");
// root_element.id = styles.phantasmal_world_root;
// document.body.append(root_element);
//
// ReactDOM.render(<ApplicationComponent />, root_element);

initialize();
