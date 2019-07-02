import { observable } from "mobx";
import { Server } from "../domain";

class ApplicationStore {
    @observable current_server: Server = Server.Ephinea;
}

export const application_store = new ApplicationStore();
