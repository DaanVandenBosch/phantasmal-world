import { Item } from "../../domain";
import { sortedUniq } from "lodash";

export async function getItems(server: string): Promise<Item[]> {
    const response = await fetch(process.env.PUBLIC_URL + `/drops.${server}.tsv`);
    const data = await response.text();
    return sortedUniq(
        data.split('\n').slice(1).map(line => line.split('\t')[4]).sort()
    ).map(name => new Item(name));
}
