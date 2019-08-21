import { View } from "./View";
import { WritableProperty } from "../observable/WritableProperty";
import { property } from "../observable";

export abstract class Control extends View {
    readonly enabled: WritableProperty<boolean> = property(true);
}
