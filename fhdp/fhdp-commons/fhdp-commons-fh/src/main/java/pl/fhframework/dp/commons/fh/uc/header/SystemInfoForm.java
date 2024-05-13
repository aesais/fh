package pl.fhframework.dp.commons.fh.uc.header;

import lombok.Getter;
import lombok.Setter;
import pl.fhframework.core.model.Model;
import pl.fhframework.model.forms.Form;

import java.util.ArrayList;
import java.util.List;

@Model
public class SystemInfoForm extends Form<SystemInfoForm.Model> {
    @Getter
    @Setter
    public static class Model {
        private String user;
        private String env;
        private String version;
        private String subversion;
        private String serverIP;
        private String roles;
        private String orgUnitCode;
        private String orgUnitName;
        private String office = "Office";
        private List<String> altOffices = new ArrayList<>();
        private String counter = "<span id=\"sessionCounter\">--:--</span>";
    }

    public SystemInfoForm() {
        setStyleClasses("form-transparent");
    }
}
