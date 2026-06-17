<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<Form xmlns="http://fh.asseco.com/form/1.0" container="mainForm" formType="STANDARD" modalSize="REGULAR">
    <AvailabilityConfiguration>
        <Preview when="fileScan==null">activityId,incidentId,selectedActivityId</Preview>
    </AvailabilityConfiguration>
    <TabContainer>
        <Tab label="tab1">
            <PanelGroup label="Widok BPMN">
                <CamundaProcessInstanceViewer id="camundaProcessInstViewer"
                                              availability="EDIT"
                                              processDefinitionBpmn="{processDefinitionBpmn}"
                                              bpmnActivityIds="{bpmnActivityIds}"
                                              incidentBpmnActivityIds="{incidentBpmnActivityIds}"
                                              selectedBpmnElementIdsChain="{selectedActivityId}"
                                              height="200px"
                                              zoomInBtnHint="{zoomInBtnHint}"
                                              zoomOutBtnHint="Pomniejsz"
                                              zoomResetBtnHint="Resetuj zoom"


                />
            </PanelGroup>
            <PanelGroup label="Parametry">
                <FileUpload id="fileUploadButton" verticalAlign="bottom" width="md-3" file="{fileScan}" label="Dodaj plik bpmn" onUpload="fileUploadMenu" maxSize="15728640" labelHidden="true" extensions=".bpmn"/>
                <InputText id="activityId" verticalAlign="bottom" label="Id aktywności (rozdzielone przecinkiem)" width="md-3" value="{activityIdRaw}" onChange="-" onInput="-"/>
                <InputText id="incidentId" verticalAlign="bottom" label="Id incydentu (rozdzielone przecinkiem)" width="md-3" value="{incidentIdRaw}" onChange="-" onInput="-"/>
                <InputText id="selectedActivityId" verticalAlign="bottom" label="Zaznaczona aktywność" width="md-3" value="{selectedActivityId}" onChange="-" onInput="-"/>
                <Button label="Odśwież" onClick="refresh"/>
            </PanelGroup>
            <Table label="bpmns" collection="{bpmns}" iterator="item" selected="{processDefinitionBpmn}">
                <Column id="col1" label="col1" value="item1"/>
                <Column id="col2" label="col2" value="item2"/>
                <Column id="col3" label="col3" value="item3"/>
                <Column id="col4" label="col4" value="item4"/>
                <Column id="col5" label="col5" value="item5"/>
            </Table>
        </Tab>
        <Tab label="tab2">
            <Button label="Odśwież" onClick="refresh" hint="Odświeża formularz"/>
            <Button label="Odśwież" onClick="refresh" hint="Odświeża formularz"/>
            <TabContainer>
                <Tab>
                    <Button label="Odśwież" onClick="refresh"/>
                </Tab>
            </TabContainer>
        </Tab>
    </TabContainer>
</Form>
