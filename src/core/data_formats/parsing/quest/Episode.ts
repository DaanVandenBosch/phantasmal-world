export enum Episode {
    I = 1,
    II = 2,
    IV = 4,
}

export const EPISODES = [Episode.I, Episode.II, Episode.IV];

export function check_episode(episode: Episode): void {
    if (Episode[episode] == undefined) {
        throw new Error(`Invalid episode ${episode}.`);
    }
}
