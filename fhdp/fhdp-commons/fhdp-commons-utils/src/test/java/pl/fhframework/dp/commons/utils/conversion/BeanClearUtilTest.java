package pl.fhframework.dp.commons.utils.conversion;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import pl.fhframework.dp.commons.utils.conversion.model.ComplexObject;
import pl.fhframework.dp.commons.utils.conversion.model.MockObject;

import java.time.LocalDateTime;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestConfig.class})
@EnableConfigurationProperties
@Slf4j
public class BeanClearUtilTest {

    @Autowired
    MockObject mockObject;

    @Test
    public void test() throws IllegalAccessException {
        BeanClearUtil.clearObject(mockObject);
        /**
         * The below objects are set as empty collections,
         * so should be cleared up
         */
        Assert.assertNull(mockObject.getEmptyNestedObject());
        Assert.assertNull(mockObject.getNestedObject().getItemEmptyArray());
        Assert.assertNull(mockObject.getNestedObject().getItemEmptyCollection());
        Assert.assertNull(mockObject.getNestedObject().getItemEmptyList());
        Assert.assertNull(mockObject.getNestedObject().getItemEmptyMap());

        /**
         * The below objects are set with content,
         * so shouldn't be cleared up
         */
        Assert.assertNotNull(mockObject.getNestedObject());
        Assert.assertNotNull(mockObject.getNestedObjects());
        Assert.assertNotNull(mockObject.getContent());
        Assert.assertNotNull(mockObject.getNestedObjects().get(0).getContent());
    }

    @Test
    public void notRemoveDocumentListInComplexObjectWithDocumentPropArgElements() throws IllegalAccessException {
        // SETUP
        ComplexObject complexObject = new ComplexObject();

        ComplexObject.DocumentObject documentObject = new ComplexObject.DocumentObject();
        complexObject.getDocumentObjectList().add(documentObject);
        documentObject.setDocumentType(ComplexObject.DocumentType.ONE);

        ComplexObject.DocumentProp documentProp = new ComplexObject.DocumentProp();
        documentObject.setDocumentProp(documentProp);
        documentProp.setPropType(ComplexObject.DocumentPropType.PROP_A);

        ComplexObject.DocumentPropArg documentPropArg = new ComplexObject.DocumentPropArg();
        documentProp.getDocumentPropArgList().add(documentPropArg);
        documentPropArg.setLongValue(1L);
        documentPropArg.setStringValue("VALUE");

        // EXECUTE
        BeanClearUtil.clearObject(complexObject);

        // ASSERT
        Assert.assertNotNull(complexObject);
        Assert.assertEquals(complexObject.getDocumentObjectList().size(), 1);
        ComplexObject.DocumentObject verificationDocumentObject = complexObject.getDocumentObjectList().get(0);
        Assert.assertEquals(verificationDocumentObject.getDocumentType(), ComplexObject.DocumentType.ONE);
        Assert.assertNotNull(verificationDocumentObject.getDocumentProp());
        Assert.assertEquals(verificationDocumentObject.getDocumentProp().getPropType(), ComplexObject.DocumentPropType.PROP_A);
        Assert.assertEquals(verificationDocumentObject.getDocumentProp().getDocumentPropArgList().size(), 1);
    }

    @Test
    public void notRemoveDocumentListInComplexObjectWithoutDocumentPropArgElements() throws IllegalAccessException {
        // SETUP
        ComplexObject complexObject = new ComplexObject();

        ComplexObject.DocumentObject documentObject = new ComplexObject.DocumentObject();
        complexObject.getDocumentObjectList().add(documentObject);
        documentObject.setDocumentType(ComplexObject.DocumentType.ONE);

        ComplexObject.DocumentProp documentProp = new ComplexObject.DocumentProp();
        documentObject.setDocumentProp(documentProp);
        documentProp.setPropType(ComplexObject.DocumentPropType.PROP_A);

        // EXECUTE
        BeanClearUtil.clearObject(complexObject);

        // ASSERT
        Assert.assertNotNull(complexObject);
        Assert.assertEquals(complexObject.getDocumentObjectList().size(), 1);
        ComplexObject.DocumentObject verificationDocumentObject = complexObject.getDocumentObjectList().get(0);
        Assert.assertEquals(verificationDocumentObject.getDocumentType(), ComplexObject.DocumentType.ONE);
        Assert.assertNotNull(verificationDocumentObject.getDocumentProp());
        Assert.assertEquals(verificationDocumentObject.getDocumentProp().getPropType(), ComplexObject.DocumentPropType.PROP_A);
    }
}
