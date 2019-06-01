import { memoize } from 'lodash';
import { huntMethodStore } from '../stores/HuntMethodStore';
import { getHuntMethods } from '../data/loading/huntMethods';

export const loadHuntMethods = memoize(
    async (server: string) => {
        huntMethodStore.methods.replace(await getHuntMethods(server));
    }
);
