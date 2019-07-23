import React from "react";
import ReactDOM from "react-dom";
import Logger from "js-logger";
import "./index.less";
import { ApplicationComponent } from "./ui/ApplicationComponent";
import "react-virtualized/styles.css";
import "react-select/dist/react-select.css";
import "react-virtualized-select/styles.css";
import "golden-layout/src/css/goldenlayout-base.css";
import "golden-layout/src/css/goldenlayout-dark-theme.css";

Logger.useDefaults({
    defaultLevel: (Logger as any)[process.env["REACT_APP_LOG_LEVEL"] || "OFF"],
});

ReactDOM.render(<ApplicationComponent />, document.getElementById("phantasmal-world-root"));
