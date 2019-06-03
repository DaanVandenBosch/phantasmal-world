import { observable } from "mobx";
import { Server } from "../domain";

class ApplicationStore {
    @observable currentServer: Server = Server.Ephinea;
}

export const applicationStore = new ApplicationStore();
