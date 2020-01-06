import { property } from "../observable";
import { Property } from "../observable/property/Property";
import { WritableProperty } from "../observable/property/WritableProperty";
import { Button, ButtonOptions } from "./Button";
import { open_files } from "../files";

export type FileButtonOptions = ButtonOptions & {
    accept?: string;
    multiple?: boolean;
};

export class FileButton extends Button {
    private readonly _files: WritableProperty<File[]> = property<File[]>([]);

    readonly files: Property<File[]> = this._files;

    constructor(options?: FileButtonOptions) {
        super(options);

        this.element.classList.add("core_FileButton");

        this.disposables(
            this.onclick.observe(async () => {
                this._files.val = await open_files(options);
            }),
        );

        this.finalize_construction();
    }
}
