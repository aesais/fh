package pl.fhframework.dp.commons.fh.uc.header;

import lombok.Getter;
import lombok.Setter;
import pl.fhframework.binding.StaticBinding;
import pl.fhframework.model.forms.Form;

public class UserInitialForm extends Form<UserInitialForm.Model> {
    @Getter
    @Setter
    public static class Model {
        private String userInitial;
    }

    public UserInitialForm(){
//        setStyleClassesBinding(new StaticBinding<>("form-transparent"));
    }
}