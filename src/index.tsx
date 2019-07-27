import React from "react";
import ReactDOM from "react-dom";
import Logger from "js-logger";
import styles from "./ui/index.css";
import { ApplicationComponent } from "./ui/ApplicationComponent";
import "react-virtualized/styles.css";
import "react-select/dist/react-select.css";
import "react-virtualized-select/styles.css";
import "golden-layout/src/css/goldenlayout-base.css";
import "golden-layout/src/css/goldenlayout-dark-theme.css";
import "antd/dist/antd.less";

Logger.useDefaults({
    defaultLevel: (Logger as any)[process.env["LOG_LEVEL"] || "OFF"],
});

const root_element = document.createElement("div");
root_element.id = styles.phantasmal_world_root;
document.body.append(root_element);

ReactDOM.render(<ApplicationComponent />, root_element);
