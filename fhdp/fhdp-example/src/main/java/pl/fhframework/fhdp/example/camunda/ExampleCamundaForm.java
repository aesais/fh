package pl.fhframework.fhdp.example.camunda;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.Resource;
import pl.fhframework.core.util.StringUtils;
import pl.fhframework.model.forms.Form;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExampleCamundaForm extends Form<ExampleCamundaForm.Model> {
    @Data
    public static class Model {
        private String processDefinitionBpmn;
        private List<String> bpmnActivityIds;
        private List<String> incidentBpmnActivityIds;
        private String selectedActivityId;

        private Resource fileScan;

        private String activityIdRaw;
        private String incidentIdRaw;

        private String zoomInBtnHint = "Przybliż";

        private List<String> bpmns = new ArrayList<>();

        public void setActivityIdRaw(String val) {
            if (StringUtils.isNullOrEmpty(val))
                bpmnActivityIds = new ArrayList<>();
            else
                bpmnActivityIds = Arrays.asList(val.split(","));
            activityIdRaw = val;
        }

        public void setIncidentIdRaw(String val) {
            if (StringUtils.isNullOrEmpty(val))
                incidentBpmnActivityIds = new ArrayList<>();
            else
                incidentBpmnActivityIds = Arrays.asList(val.split(","));
            incidentIdRaw = val;
        }
    }
}
