package pl.fhframework.dp.commons.utils.conversion.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ComplexObject {
    private String stringEmptyValue;
    private List<DocumentObject> documentObjectList = new ArrayList<>();

    public List<DocumentObject> getDocumentObjectList() {
        if (documentObjectList == null) {
            documentObjectList = new ArrayList<>();
        }
        return this.documentObjectList;
    }

    @Data
    public static class DocumentObject {
        private String stringEmptyValue;
        private DocumentType documentType;
        private DocumentProp documentProp;
    }

    @Data
    public static class DocumentProp {
        private DocumentPropType propType;
        private List<DocumentPropArg> documentPropArgList = new ArrayList<>();

        public List<DocumentPropArg> getDocumentPropArgList() {
            if (documentPropArgList == null) {
                documentPropArgList = new ArrayList<>();
            }
            return this.documentPropArgList;
        }
    }

    @Data
    public static class DocumentPropArg {
        private String stringEmptyValue;
        private Long longValue;
        private String stringValue;
    }


    public enum DocumentType {
        ONE, TWO
    }

    public enum DocumentPropType {
        PROP_A, PROP_B
    }
}
