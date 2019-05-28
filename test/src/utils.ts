import * as fs from 'fs';

/**
 * Applies f to all QST files in a directory.
 * F is called with the path to the file, the file name and the content of the file.
 * Uses the QST files provided with Tethealla version 0.143 by default.
 */
export function walkQstFiles(
    f: (path: string, fileName: string, contents: Buffer) => void,
    dir: string = 'test/resources/tethealla_v0.143_quests'
) {
    for (const [path, file] of getQstFiles(dir)) {
        f(path, file, fs.readFileSync(path));
    }
}

export function getQstFiles(dir: string): [string, string][] {
    let files: [string, string][] = [];

    for (const file of fs.readdirSync(dir)) {
        const path = `${dir}/${file}`;
        const stats = fs.statSync(path);

        if (stats.isDirectory()) {
            files = files.concat(getQstFiles(path));
        } else if (path.endsWith('.qst')) {
            // BUG: Battle quests are not always parsed in the same way.
            // Could be a bug in Jest or Node as the quest parsing code has no randomness or dependency on mutable state.
            // TODO: Some quests can not yet be parsed correctly.
            const exceptions = [
                '/battle/', // Battle mode quests
                '/princ/', // Goverment quests
                'fragmentofmemoryen.qst',
                'lost havoc vulcan.qst',
                'ep2/event/ma4-a.qst',
                'gallon.qst',
                'ep1/04.qst',
                'goodluck.qst'
            ];

            if (exceptions.every(e => path.indexOf(e) === -1)) {
                files.push([path, file]);
            }
        }
    }

    return files;
}
