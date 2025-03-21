import {injectable} from "inversify";
import getDecorators from "inversify-inject-decorators";
import {BaseEvent} from "./BaseEvent";
import {FormsManager} from "../Socket/FormsManager";
import {FhContainer} from "../FhContainer";


@injectable()
class CloseWindowEvent extends BaseEvent {


    constructor() {
        super();
    }

    public fire(data) {
       window.close();
    }
}

export {CloseWindowEvent};