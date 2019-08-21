import { ApplicationView } from "./application/gui/ApplicationView";
import { Disposable } from "./core/observable/Disposable";
import "./index.css";
import { throttle } from "lodash";

export function initialize(): Disposable {
    const application_view = new ApplicationView();

    const resize = throttle(
        () => {
            application_view.resize(window.innerWidth, window.innerHeight);
        },
        100,
        { leading: true, trailing: true },
    );

    resize();
    document.body.append(application_view.element);
    window.addEventListener("resize", resize);

    return {
        dispose(): void {
            window.removeEventListener("resize", resize);
            application_view.dispose();
        },
    };
}
