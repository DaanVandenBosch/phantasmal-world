export async function read_file(file: File): Promise<ArrayBuffer> {
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
