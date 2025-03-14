import {injectable} from "inversify";
import {BaseEvent} from "./BaseEvent";
import {Util} from "../Util";
import getDecorators from "inversify-inject-decorators";
import {FhContainer} from "../FhContainer";
import {ConversationHandler} from "../Socket/ConversationHandler";
let { lazyInject } = getDecorators(FhContainer);

@injectable()
class ForcedLogoutEvent extends BaseEvent {
    @lazyInject("Util")
    protected util: Util;

    @lazyInject("ConversationHandler")
    protected conversationHandler: ConversationHandler;

    public fire(data) {
        this.conversationHandler.clearData();
        window.location.href = this.util.getPath('autologout?reason=') + data.reason;
    }
}

export { ForcedLogoutEvent };