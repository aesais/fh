import {injectable} from "inversify";
import {BaseEvent} from "./BaseEvent";
import {Util} from "../Util";
import getDecorators from "inversify-inject-decorators";
import {FhContainer} from "../FhContainer";
import {ConversationHandler} from "../Socket/ConversationHandler";
import {Connector} from "../Socket/Connector";
import {ForcedLogutEventPL} from "./i18n/ForcedLogutEvent.pl";
import {ForcedLogutEventEN} from "./i18n/ForcedLogoutEvent.en";

let { lazyInject } = getDecorators(FhContainer);

@injectable()
class ForcedLogoutEvent extends BaseEvent {
    @lazyInject("Util")
    protected util: Util;

    @lazyInject("Connector")
    protected connector: Connector;

    @lazyInject("ConversationHandler")
    protected conversationHandler: ConversationHandler;

    constructor() {
        super();
        this.i18n.registerStrings('pl', ForcedLogutEventPL);
        this.i18n.registerStrings('en', ForcedLogutEventEN);
    }

    public fire(data) {
        this.conversationHandler.clearData();
        this.connector.close();
        this.util.showDialog(this.i18n.__('forcedLogout.title'),
            this.i18n.__('forcedLogout.message'),
            this.i18n.__('forcedLogout.button'),
            'btn-primary',
            function() { location.reload(); }
        );
    }
}

export { ForcedLogoutEvent };