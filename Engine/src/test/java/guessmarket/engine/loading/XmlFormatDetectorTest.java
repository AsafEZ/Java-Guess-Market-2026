package guessmarket.engine.loading;

import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlFormatDetectorTest {
    private final XmlFormatDetector detector = new XmlFormatDetector();

    @Test
    void detectsValidAssignmentOneDocument(@TempDir Path tempDir) throws Exception {
        Path xml = writeXml(tempDir, "legacy.xml", assignmentOneXml(null));

        assertEquals(XmlFormatVersion.ASSIGNMENT_1, detector.detect(xml));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "assignment2/xml/valid/small.xml",
            "assignment2/xml/valid/multiple.xml",
            "assignment2/xml/edge-cases/schema-boundaries.xml"
    })
    void detectsAssignmentTwoFixtures(String resourcePath) throws Exception {
        assertEquals(
                XmlFormatVersion.ASSIGNMENT_2,
                detector.detect(resourcePath(resourcePath)));
    }

    @Test
    void unknownSchemaLocationDoesNotOverrideStructure(@TempDir Path tempDir)
            throws Exception {
        Path xml = writeXml(
                tempDir,
                "misleading.xml",
                assignmentTwoXml("untrusted-or-missing-schema.xsd", "commission"));

        assertEquals(XmlFormatVersion.ASSIGNMENT_2, detector.detect(xml));
    }

    @Test
    void assignmentOneStructureRejectsAssignmentTwoSchemaLocation(@TempDir Path tempDir)
            throws Exception {
        Path xml = writeXml(
                tempDir,
                "legacy-wrong-schema.xml",
                assignmentOneXml("GM-EX2-Schema.xsd"));

        assertXmlParseError(xml);
    }

    @Test
    void assignmentTwoStructureRejectsAssignmentOneSchemaLocation(@TempDir Path tempDir)
            throws Exception {
        Path xml = writeXml(
                tempDir,
                "v2-wrong-schema.xml",
                assignmentTwoXml("GM-EX1-Schema.xsd", "commission"));

        assertXmlParseError(xml);
    }

    @Test
    void hybridDocumentIsRejected(@TempDir Path tempDir) throws Exception {
        Path xml = writeXml(
                tempDir,
                "hybrid.xml",
                assignmentTwoXml(null, "comision"));

        EngineException exception = assertXmlParseError(xml);
        assertTrue(exception.getMessage().contains("mixes"));
    }

    @Test
    void nestedAssignmentTwoMarkerDoesNotChangeRootLevelDetection(@TempDir Path tempDir)
            throws Exception {
        String xmlText = assignmentOneXml(null)
                .replace(
                        "<description>Legacy event</description>",
                        "<description><GM-users/></description>");
        Path xml = writeXml(tempDir, "nested-marker.xml", xmlText);

        assertEquals(XmlFormatVersion.ASSIGNMENT_1, detector.detect(xml));
    }

    @Test
    void wrongRootIsRejected(@TempDir Path tempDir) throws Exception {
        Path xml = writeXml(
                tempDir,
                "wrong-root.xml",
                "<Not-Guess-Market><GM-events/></Not-Guess-Market>");

        assertXmlParseError(xml);
    }

    @Test
    void malformedXmlIsRejected(@TempDir Path tempDir) throws Exception {
        Path xml = writeXml(
                tempDir,
                "malformed.xml",
                "<Guess-Market><GM-events></Guess-Market>");

        assertXmlParseError(xml);
    }

    @Test
    void doctypeIsRejected(@TempDir Path tempDir) throws Exception {
        Path xml = writeXml(
                tempDir,
                "doctype.xml",
                """
                        <?xml version="1.0"?>
                        <!DOCTYPE Guess-Market [<!ELEMENT Guess-Market ANY>]>
                        <Guess-Market><GM-events/></Guess-Market>
                        """);

        EngineException exception = assertXmlParseError(xml);
        assertTrue(
                exception.getMessage().contains("DOCTYPE")
                        || exception.getCause() != null);
    }

    @Test
    void externalEntityIsRejectedWithoutReadingItsTarget(@TempDir Path tempDir)
            throws Exception {
        Path external = tempDir.resolve("must-not-be-read.txt");
        Files.writeString(external, "EXTERNAL_SECRET_MARKER");
        String xmlText = """
                <?xml version="1.0"?>
                <!DOCTYPE Guess-Market [<!ENTITY xxe SYSTEM "%s">]>
                <Guess-Market><GM-events><value>&xxe;</value></GM-events></Guess-Market>
                """.formatted(external.toUri());
        Path xml = writeXml(tempDir, "xxe.xml", xmlText);

        EngineException exception = assertXmlParseError(xml);
        assertTrue(!exception.getMessage().contains("EXTERNAL_SECRET_MARKER"));
    }

    @Test
    void missingPathUsesExistingFileNotFoundError(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("missing.xml");

        EngineException exception = assertThrows(
                EngineException.class,
                () -> detector.detect(missing));
        assertEquals(ErrorCode.FILE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void repeatedCallsDoNotShareDetectionState(@TempDir Path tempDir) throws Exception {
        Path assignmentOne = writeXml(tempDir, "one.xml", assignmentOneXml(null));
        Path assignmentTwo = writeXml(
                tempDir,
                "two.xml",
                assignmentTwoXml(null, "commission"));

        assertEquals(XmlFormatVersion.ASSIGNMENT_1, detector.detect(assignmentOne));
        assertEquals(XmlFormatVersion.ASSIGNMENT_2, detector.detect(assignmentTwo));
        assertEquals(XmlFormatVersion.ASSIGNMENT_1, detector.detect(assignmentOne));
    }

    private EngineException assertXmlParseError(Path xml) {
        EngineException exception = assertThrows(
                EngineException.class,
                () -> detector.detect(xml));
        assertEquals(ErrorCode.XML_PARSE_ERROR, exception.getErrorCode());
        return exception;
    }

    private static Path resourcePath(String resourcePath) throws URISyntaxException {
        URL resource = XmlFormatDetectorTest.class.getClassLoader().getResource(resourcePath);
        if (resource == null) {
            throw new AssertionError("Missing classpath resource: " + resourcePath);
        }
        return Path.of(resource.toURI());
    }

    private static Path writeXml(Path directory, String fileName, String content)
            throws Exception {
        Path path = directory.resolve(fileName);
        Files.writeString(path, content);
        return path;
    }

    private static String assignmentOneXml(String schemaLocation) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Guess-Market%s>
                    <GM-events>
                        <GM-event name="Legacy">
                            <id>1</id>
                            <description>Legacy event</description>
                            <comision type="on-purchase">5</comision>
                            <GM-options><GM-option>Yes</GM-option><GM-option>No</GM-option></GM-options>
                            <GM-method><GM-LMSR><b>10</b></GM-LMSR></GM-method>
                        </GM-event>
                    </GM-events>
                </Guess-Market>
                """.formatted(schemaAttribute(schemaLocation));
    }

    private static String assignmentTwoXml(String schemaLocation, String commissionElement) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Guess-Market%s>
                    <GM-events>
                        <GM-event name="Task 2">
                            <id>1</id>
                            <description>Task 2 event</description>
                            <%s type="on-purchase">5</%s>
                            <GM-options><GM-option>Yes</GM-option><GM-option>No</GM-option></GM-options>
                            <GM-method><GM-LMSR><b>10</b></GM-LMSR></GM-method>
                        </GM-event>
                    </GM-events>
                    <GM-users>
                        <GM-user name="Alice"><initial-cash>100</initial-cash></GM-user>
                    </GM-users>
                </Guess-Market>
                """.formatted(
                schemaAttribute(schemaLocation),
                commissionElement,
                commissionElement);
    }

    private static String schemaAttribute(String schemaLocation) {
        if (schemaLocation == null) {
            return "";
        }
        return " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xsi:noNamespaceSchemaLocation=\"" + schemaLocation + "\"";
    }
}
