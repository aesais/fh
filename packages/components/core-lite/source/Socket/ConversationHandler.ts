import {injectable} from "inversify";
import {ConversationType} from "./ConversationType";

/**
 *
 */
@injectable()
export class ConversationHandler {

    public setConnectionData(conversationData:ConversationType, connectionType:ConnectionTypeEnum = ConnectionTypeEnum.FH_CONNECTION):ConversationType {

        connectionType = this.resolveConectionType(connectionType);

        let storedConversationData: ConversationType = this.getConnectionData(connectionType);

        if(storedConversationData && storedConversationData.conversationId == conversationData.conversationId && storedConversationData.sessionId == conversationData.sessionId){
            //do nothing
        } else {
            //Override existing data.
            sessionStorage.setItem(connectionType, JSON.stringify(conversationData));
        }


        return conversationData

    }


    public getConnectionData(connectionType:ConnectionTypeEnum = ConnectionTypeEnum.FH_CONNECTION):ConversationType {
        connectionType = this.resolveConectionType(connectionType);
        return JSON.parse(sessionStorage.getItem(connectionType))
    }

    private resolveConectionType(connectionType: ConnectionTypeEnum){
        if(connectionType == ConnectionTypeEnum.FH_CONNECTION) {
            if(this.isPopup()){
                return ConnectionTypeEnum.FH_CONNECTION_POPUP
            }

        } else if (connectionType == ConnectionTypeEnum.FH_CONNECTION_EXTERNAL){
            if(this.isPopup()){
                return ConnectionTypeEnum.FH_CONNECTION_EXTERNAL_POPUP
            }
        }

        return connectionType;
    }


    private isPopup():boolean {
        if(window.opener != null){
            return true
        } else {
            return false;
        }
    }

    public clearData(){
        sessionStorage.clear();
    }

}

export enum ConnectionTypeEnum {
    FH_CONNECTION = "fh_connection",
    FH_CONNECTION_EXTERNAL = "fh_connection_external",
    FH_CONNECTION_EXTERNAL_POPUP = "fh_connection_external_popup",
    FH_CONNECTION_POPUP = "fh_connection_popup"
}

