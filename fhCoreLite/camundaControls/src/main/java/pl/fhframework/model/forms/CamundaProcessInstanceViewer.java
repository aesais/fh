package pl.fhframework.model.forms;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import pl.fhframework.BindingResult;
import pl.fhframework.annotations.*;
import pl.fhframework.binding.ModelBinding;
import pl.fhframework.model.dto.ElementChanges;

import java.util.List;
import java.util.Objects;


@Control(parents = {PanelGroup.class, Tab.class, Row.class, Form.class, Group.class}, canBeDesigned = true)
@DocumentedComponent(category = DocumentedComponent.Category.CAMUNDA, documentationExample = true, value = "Camunda process instance viewer resembling original Camunda cockpit", icon = "fa fa-project-diagram")
@DesignerControl(defaultWidth = 6)
public class CamundaProcessInstanceViewer extends FormElement {

    private static final String ATTR_ACTIVITY_IDS = "bpmnActivityIds";
    protected static final String ATTR_INCIDENT_ACTIVITY_IDS = "incidentBpmnActivityIds";
    protected static final String ATTR_SELECTED_ACTIVITY_ID = "selectedBpmnElementIdsChain";
    protected static final String ATTR_PROCESS_DEFINITION_BPMN = "processDefinitionBpmn";


    protected static final String ATTR_ZOOM_IN_BTN_HINT = "zoomInBtnHint";
    protected static final String ATTR_ZOOM_OUT_BTN_HINT = "zoomOutBtnHint";
    protected static final String ATTR_ZOOM_RESET_BTN_HINT = "zoomResetBtnHint";
    protected static final String ATTR_NAVIGATION_CONTROLS_VISIBLE = "navigationControlsVisible";

    @Getter
    private List<String> selectedBpmnElementIdsChain;

    @Getter
    private List<String> bpmnActivityIds;

    @Getter
    private List<String> incidentBpmnActivityIds;

    @Getter
    private String processDefinitionBpmn;


    @Getter
    private String zoomInBtnHint;
    @Getter
    private String zoomOutBtnHint;
    @Getter
    private String zoomResetBtnHint;
    @Getter
    private String navigationControlsVisible;


    @JsonIgnore
    @Getter
    @Setter
    @XMLProperty(value = ATTR_ACTIVITY_IDS)
    @DesignerXMLProperty(allowedTypes = List.class)
    @DocumentedComponentAttribute(boundable = true, value = "List<String> contains lists of bpmnActivityIds that will be marked in the viewer with standard badge ")
    private ModelBinding<List<String>> bpmnActivityIdsBinding;

    @JsonIgnore
    @Getter
    @Setter
    @XMLProperty(value = ATTR_INCIDENT_ACTIVITY_IDS)
    @DesignerXMLProperty(allowedTypes = List.class)
    @DocumentedComponentAttribute(boundable = true, value = "List<String> contains list of incidentBpmnActivityIds that will be marked in the viewer with badge incident")
    private ModelBinding<List<String>> incidentBpmnActivityIdsBinding;


    @JsonIgnore
    @Getter
    @Setter
    @XMLProperty(value = ATTR_SELECTED_ACTIVITY_ID)
    @DesignerXMLProperty(allowedTypes = List.class)
    @DocumentedComponentAttribute(boundable = true, value = "List<String> contains selected selectedBpmnElementIdsChain, where chain allows to automatically drill to activity (or activityIncident) the viewer will focus on")
    private ModelBinding<List<String>> selectedBpmnElementIdsChainBinding;


    @JsonIgnore
    @Getter
    @Setter
    @XMLProperty(value = ATTR_PROCESS_DEFINITION_BPMN)
    @DocumentedComponentAttribute(boundable = true, value = "Process definition BPMN")
    private ModelBinding<String> processDefinitionBpmnBinding;



    @JsonIgnore
    @Getter
    @Setter
    @XMLProperty(value = ATTR_ZOOM_IN_BTN_HINT)
    @DocumentedComponentAttribute(boundable = true, value = "Zoom in btn hint")
    private ModelBinding<String> zoomInBtnHintBinding;

    @JsonIgnore
    @Getter
    @Setter
    @XMLProperty(value = ATTR_ZOOM_OUT_BTN_HINT)
    @DocumentedComponentAttribute(boundable = true, value = "Zoom out btn hint")
    private ModelBinding<String> zoomOutBtnHintBinding;

    @JsonIgnore
    @Getter
    @Setter
    @XMLProperty(value = ATTR_ZOOM_RESET_BTN_HINT)
    @DocumentedComponentAttribute(boundable = true, value = "Zoom out btn hintN")
    private ModelBinding<String> zoomResetBtnHintBinding;

    @JsonIgnore
    @Getter
    @Setter
    @XMLProperty(value = ATTR_NAVIGATION_CONTROLS_VISIBLE)
    @DocumentedComponentAttribute(boundable = true, value = "Is navigation controls visible")
    private ModelBinding<String> navigationControlsVisibleBinding;



    public CamundaProcessInstanceViewer(Form form) {
        super(form);
    }

    @Override
    public void init() {
        super.init();


        if (selectedBpmnElementIdsChainBinding != null) {
            BindingResult<List<String>> selectedBpmnElementIdsChainBindingResult = selectedBpmnElementIdsChainBinding.getBindingResult();
            if (selectedBpmnElementIdsChainBindingResult != null) {
                if (!Objects.equals(this.selectedBpmnElementIdsChain, selectedBpmnElementIdsChainBindingResult.getValue()))
                    this.selectedBpmnElementIdsChain = selectedBpmnElementIdsChainBindingResult.getValue();

            }
        }
        if (processDefinitionBpmnBinding != null) {
            BindingResult<String> processDefinitionBpmnBindingResult = processDefinitionBpmnBinding.getBindingResult();
            if (processDefinitionBpmnBindingResult != null) {
                if (!Objects.equals(this.processDefinitionBpmn, processDefinitionBpmnBindingResult.getValue()))
                    this.processDefinitionBpmn = processDefinitionBpmnBindingResult.getValue();

            }
        }
        if (bpmnActivityIdsBinding != null) {
            BindingResult<List<String>> bpmnActivityIdsBindingResult = bpmnActivityIdsBinding.getBindingResult();
            if (bpmnActivityIdsBindingResult != null) {
                if (!Objects.equals(this.bpmnActivityIds, bpmnActivityIdsBindingResult.getValue()))
                    this.bpmnActivityIds = bpmnActivityIdsBindingResult.getValue();
            }
        }

        if (incidentBpmnActivityIdsBinding != null) {
            BindingResult<List<String>> incidentBpmnActivityIdsBindingResult = incidentBpmnActivityIdsBinding.getBindingResult();
            if (incidentBpmnActivityIdsBindingResult != null) {
                if (!Objects.equals(this.incidentBpmnActivityIds, incidentBpmnActivityIdsBindingResult.getValue()))
                    this.incidentBpmnActivityIds = incidentBpmnActivityIdsBindingResult.getValue();

            }
        }

//        ***********************************

        if (zoomInBtnHintBinding != null) {
            BindingResult<String> zoomInBtnHintBindingResult = zoomInBtnHintBinding.getBindingResult();
            if (zoomInBtnHintBindingResult != null) {
                if (!Objects.equals(this.zoomInBtnHint, zoomInBtnHintBindingResult.getValue()))
                    this.zoomInBtnHint = zoomInBtnHintBindingResult.getValue();

            }
        }

        if (zoomOutBtnHintBinding != null) {
            BindingResult<String> zoomOutBtnHintBindingResult = zoomOutBtnHintBinding.getBindingResult();
            if (zoomOutBtnHintBindingResult != null) {
                if (!Objects.equals(this.zoomOutBtnHint, zoomOutBtnHintBindingResult.getValue()))
                    this.zoomOutBtnHint = zoomOutBtnHintBindingResult.getValue();

            }
        }

        if (zoomResetBtnHintBinding != null) {
            BindingResult<String> zoomResetBtnHintBindingResult = zoomResetBtnHintBinding.getBindingResult();
            if (zoomResetBtnHintBindingResult != null) {
                if (!Objects.equals(this.zoomResetBtnHint, zoomResetBtnHintBindingResult.getValue()))
                    this.zoomResetBtnHint = zoomResetBtnHintBindingResult.getValue();

            }
        }

        if (navigationControlsVisibleBinding!= null) {
            BindingResult<String> navigationControlsVisibleBindingResult = navigationControlsVisibleBinding.getBindingResult();
            if (navigationControlsVisibleBindingResult != null) {
                if (!Objects.equals(this.navigationControlsVisible, navigationControlsVisibleBindingResult.getValue()))
                    this.navigationControlsVisible = navigationControlsVisibleBindingResult.getValue();

            }
        }

    }

    @Override
    protected ElementChanges updateView() {
        ElementChanges elementChanges = super.updateView();
        if (selectedBpmnElementIdsChainBinding != null) {
            BindingResult<List<String>> selectedBpmnElementIdsChainBindingResult = selectedBpmnElementIdsChainBinding.getBindingResult();
            if (selectedBpmnElementIdsChainBindingResult != null) {
                if (!Objects.equals(this.selectedBpmnElementIdsChain, selectedBpmnElementIdsChainBindingResult.getValue())) {
                    this.selectedBpmnElementIdsChain = selectedBpmnElementIdsChainBindingResult.getValue();
                    elementChanges.addChange(ATTR_SELECTED_ACTIVITY_ID, this.selectedBpmnElementIdsChain);
                }
            }
        }
        if (processDefinitionBpmnBinding != null) {
            BindingResult<String> processDefinitionBpmnBindingResult = processDefinitionBpmnBinding.getBindingResult();
            if (processDefinitionBpmnBindingResult != null) {
                if (!Objects.equals(this.processDefinitionBpmn, processDefinitionBpmnBindingResult.getValue())) {
                    this.processDefinitionBpmn = processDefinitionBpmnBindingResult.getValue();
                    elementChanges.addChange(ATTR_PROCESS_DEFINITION_BPMN, this.processDefinitionBpmn);

                }
            }
        }

        if (bpmnActivityIdsBinding != null) {
            BindingResult<List<String>> bpmnActivityIdsBindingResult = bpmnActivityIdsBinding.getBindingResult();
            if (bpmnActivityIdsBindingResult != null) {
                if (!Objects.equals(this.bpmnActivityIds, bpmnActivityIdsBindingResult.getValue())) {
                    this.bpmnActivityIds = bpmnActivityIdsBindingResult.getValue();
                    elementChanges.addChange(ATTR_ACTIVITY_IDS, this.bpmnActivityIds);
                }
            }
        }

        if (incidentBpmnActivityIdsBinding != null) {
            BindingResult<List<String>> incidentBpmnActivityIdsBindingResult = incidentBpmnActivityIdsBinding.getBindingResult();
            if (incidentBpmnActivityIdsBindingResult != null) {
                if (!Objects.equals(this.incidentBpmnActivityIds, incidentBpmnActivityIdsBindingResult.getValue())) {
                    this.incidentBpmnActivityIds = incidentBpmnActivityIdsBindingResult.getValue();
                    elementChanges.addChange(ATTR_INCIDENT_ACTIVITY_IDS, this.incidentBpmnActivityIds);
                }
            }
        }
        return elementChanges;
    }
}
