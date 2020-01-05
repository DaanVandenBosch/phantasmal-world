import { input } from "./gui/dom";

export function open_files(options?: { accept?: string; multiple?: boolean }): Promise<File[]> {
    return new Promise((resolve) => {
        const el = input({ type: "file" });
        el.accept = options?.accept ?? "";
        el.multiple = options?.multiple ?? false;

        el.onchange = () => {
            if (el.files && el.files.length) {
                resolve([...el.files]);
            } else {
                resolve([]);
            }
        };

        el.click();
    });
}

export function read_file(file: File): Promise<ArrayBuffer> {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();

        reader.addEventListener("loadend", () => {
            if (reader.result instanceof ArrayBuffer) {
                resolve(reader.result);
            } else {
                reject(new Error("Couldn't read file."));
            }
        });

        reader.readAsArrayBuffer(file);
    });
}
