import './source/Module.css';

import {CamundaProcessInstanceViewer} from './source/controls/CamundaProcessInstanceViewer';
import {FhModule, FhContainer} from "fh-forms-handler";
class CamundaControls extends FhModule {
    protected registerComponents() {
        FhContainer.bind<(componentObj: any, parent: any) => CamundaProcessInstanceViewer>("CamundaProcessInstanceViewer")
            .toFactory<CamundaProcessInstanceViewer>(() => {
                return (componentObj: any, parent: any) => {
                    return new CamundaProcessInstanceViewer(componentObj, parent);
                };
            });

        console.log(`camundaControls registered components`)
    }
}

export {CamundaControls, CamundaProcessInstanceViewer}
