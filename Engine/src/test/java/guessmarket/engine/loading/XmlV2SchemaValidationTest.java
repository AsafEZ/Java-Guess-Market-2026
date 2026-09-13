package guessmarket.engine.loading;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class XmlV2SchemaValidationTest {
    private static final String SCHEMA_RESOURCE = "GM-EX2-Schema.xsd";

    private static final List<SchemaCase> VALID_CASES = List.of(
            new SchemaCase("assignment2/xml/valid/small.xml", "instructor small example"),
            new SchemaCase("assignment2/xml/valid/multiple.xml", "instructor multiple example"),
            new SchemaCase(
                    "assignment2/xml/invalid-business-rules/error-2.xml",
                    "initial-cash zero is a future business-validation failure"),
            new SchemaCase(
                    "assignment2/xml/invalid-business-rules/error-3.xml",
                    "missing Market Maker event reference is a future business-validation failure"),
            new SchemaCase(
                    "assignment2/xml/edge-cases/schema-boundaries.xml",
                    "schema-level business boundaries remain valid"));

    private static final List<SchemaCase> INVALID_CASES = List.of(
            new SchemaCase("missing-users.xml", "required GM-users is absent"),
            new SchemaCase("missing-events.xml", "required GM-events is absent"),
            new SchemaCase("misspelled-commission.xml", "commission uses the v1 spelling"),
            new SchemaCase("multiple-trading-methods.xml", "GM-method violates xs:choice"),
            new SchemaCase("missing-order-book-attribute.xml", "required order-book d is absent"),
            new SchemaCase("invalid-allow-mint.xml", "allow-mint is outside its enumeration"),
            new SchemaCase("too-many-options.xml", "GM-options contains more than two options"),
            new SchemaCase("invalid-event-sequence.xml", "GM-event children are out of sequence"),
            new SchemaCase("non-integer-event-id.xml", "event id is not an xs:int"));

    private static Schema schema;

    @BeforeAll
    static void compileSchemaFromClasspath() throws Exception {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        try (InputStream stream = openResource(SCHEMA_RESOURCE)) {
            schema = factory.newSchema(new StreamSource(stream));
        }
    }

    @TestFactory
    Stream<DynamicTest> schemaValidDocumentsPass() {
        return VALID_CASES.stream()
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.displayName(),
                        () -> assertDoesNotThrow(
                                () -> validate(testCase.resourcePath()),
                                () -> testCase.resourcePath() + " must be schema-valid")));
    }

    @TestFactory
    Stream<DynamicTest> schemaInvalidDocumentsFailForTheirNamedRule() {
        return INVALID_CASES.stream()
                .map(testCase -> DynamicTest.dynamicTest(
                        testCase.displayName(),
                        () -> {
                            assertWellFormed(testCase.resourcePath());
                            assertThrows(
                                    SAXException.class,
                                    () -> validate(invalidSchemaPath(testCase.resourcePath())),
                                    () -> testCase.resourcePath() + " must be rejected by the XSD");
                        }));
    }

    private static void validate(String resourcePath) throws Exception {
        Validator validator = schema.newValidator();
        validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        try (InputStream stream = openResource(resourcePath)) {
            validator.validate(new StreamSource(stream));
        }
    }

    private static void assertWellFormed(String fixtureName) throws Exception {
        String resourcePath = invalidSchemaPath(fixtureName);
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);

        try (InputStream stream = openResource(resourcePath)) {
            XMLStreamReader reader = factory.createXMLStreamReader(stream);
            try {
                while (reader.hasNext()) {
                    reader.next();
                }
            } finally {
                reader.close();
            }
        }
    }

    private static InputStream openResource(String resourcePath) {
        InputStream stream = XmlV2SchemaValidationTest.class
                .getClassLoader()
                .getResourceAsStream(resourcePath);
        if (stream == null) {
            return fail("Missing classpath resource: " + resourcePath);
        }
        return stream;
    }

    private static String invalidSchemaPath(String fixtureName) {
        return "assignment2/xml/invalid-schema/" + fixtureName;
    }

    private record SchemaCase(String resourcePath, String rule) {
        private String displayName() {
            return resourcePath + ": " + rule;
        }
    }
}
