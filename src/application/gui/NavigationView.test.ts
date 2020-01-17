import { NavigationView } from "./NavigationView";
import { NavigationController } from "../controllers/NavigationController";
import { GuiStore } from "../../core/stores/GuiStore";
import { StubClock } from "../../../test/src/core/StubClock";

test("Should render correctly.", () => {
    const view = new NavigationView(
        new NavigationController(new GuiStore(), new StubClock(new Date("2020-01-01T00:30:01Z"))),
    );

    expect(view.element).toMatchSnapshot(
        "It should render a button per tool, the selected server, internet time and a github link icon.",
    );
});
