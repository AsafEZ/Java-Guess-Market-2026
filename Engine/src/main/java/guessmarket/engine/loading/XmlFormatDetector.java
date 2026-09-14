package guessmarket.engine.loading;

import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

final class XmlFormatDetector {
    private static final String ROOT_ELEMENT = "Guess-Market";
    private static final String ASSIGNMENT_1_SCHEMA = "GM-EX1-Schema.xsd";
    private static final String ASSIGNMENT_2_SCHEMA = "GM-EX2-Schema.xsd";

    XmlFormatVersion detect(Path xmlPath) {
        validatePath(xmlPath);

        XMLInputFactory factory = SecureXmlInputFactory.create();
        try (InputStream stream = Files.newInputStream(xmlPath)) {
            XMLStreamReader reader = factory.createXMLStreamReader(stream);
            try {
                return detect(reader);
            } finally {
                reader.close();
            }
        } catch (EngineException ex) {
            throw ex;
        } catch (IOException | XMLStreamException ex) {
            throw xmlError("Could not inspect the XML document.", ex);
        }
    }

    private static XmlFormatVersion detect(XMLStreamReader reader)
            throws XMLStreamException {
        DetectionState state = new DetectionState();
        Deque<String> path = new ArrayDeque<>();

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.DTD) {
                throw xmlError("DOCTYPE declarations are not allowed.");
            }
            if (event == XMLStreamConstants.ENTITY_REFERENCE) {
                throw xmlError("Entity references are not allowed.");
            }
            if (event == XMLStreamConstants.START_ELEMENT) {
                inspectStartElement(reader, path, state);
                path.push(reader.getLocalName());
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                path.pop();
            }
        }

        return classify(state);
    }

    private static void inspectStartElement(
            XMLStreamReader reader,
            Deque<String> path,
            DetectionState state) {
        String localName = reader.getLocalName();

        if (path.isEmpty()) {
            if (!ROOT_ELEMENT.equals(localName) || !hasNoNamespace(reader)) {
                throw xmlError("The XML root element must be Guess-Market without a namespace.");
            }
            state.rootSeen = true;
            state.declaredVersion = declaredVersion(reader);
            return;
        }

        if (hasNoNamespace(reader) && pathMatches(path, ROOT_ELEMENT)) {
            if ("GM-events".equals(localName)) {
                state.rootEvents = true;
            } else if ("GM-users".equals(localName)) {
                state.assignment2Marker = true;
            }
            return;
        }

        if (!hasNoNamespace(reader)) {
            return;
        }

        if (pathMatches(path, "GM-event", "GM-events", ROOT_ELEMENT)) {
            if ("comision".equals(localName)) {
                state.assignment1Marker = true;
            } else if ("commission".equals(localName)) {
                state.assignment2Marker = true;
            }
        } else if (pathMatches(path, "GM-user", "GM-users", ROOT_ELEMENT)
                && "initial-cash".equals(localName)) {
            state.assignment2Marker = true;
        } else if (pathMatches(
                path, "GM-method", "GM-event", "GM-events", ROOT_ELEMENT)
                && "GM-order-book".equals(localName)) {
            state.assignment2Marker = true;
        }
    }

    private static XmlFormatVersion classify(DetectionState state) {
        if (!state.rootSeen) {
            throw xmlError("The XML document does not contain a root element.");
        }

        boolean assignment1 = state.assignment1Marker;
        boolean assignment2 = state.assignment2Marker;

        if (!assignment1 && !assignment2 && state.rootEvents) {
            assignment1 = true;
        }
        if (assignment1 && assignment2) {
            throw xmlError("The XML document mixes Assignment 1 and Assignment 2 markers.");
        }
        if (!assignment1 && !assignment2) {
            throw xmlError("The XML assignment format is ambiguous.");
        }

        XmlFormatVersion detected = assignment2
                ? XmlFormatVersion.ASSIGNMENT_2
                : XmlFormatVersion.ASSIGNMENT_1;
        if (state.declaredVersion != null && state.declaredVersion != detected) {
            throw xmlError("The XML schema location contradicts the detected document structure.");
        }
        return detected;
    }

    private static XmlFormatVersion declaredVersion(XMLStreamReader reader) {
        String location = reader.getAttributeValue(
                XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI,
                "noNamespaceSchemaLocation");
        if (location == null || location.isBlank()) {
            return null;
        }

        String fileName = schemaFileName(location.trim());
        if (ASSIGNMENT_1_SCHEMA.equals(fileName)) {
            return XmlFormatVersion.ASSIGNMENT_1;
        }
        if (ASSIGNMENT_2_SCHEMA.equals(fileName)) {
            return XmlFormatVersion.ASSIGNMENT_2;
        }
        return null;
    }

    private static String schemaFileName(String location) {
        int query = location.indexOf('?');
        int fragment = location.indexOf('#');
        int end = location.length();
        if (query >= 0) {
            end = Math.min(end, query);
        }
        if (fragment >= 0) {
            end = Math.min(end, fragment);
        }
        String path = location.substring(0, end);
        int separator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return path.substring(separator + 1);
    }

    private static boolean pathMatches(Deque<String> path, String... expected) {
        if (path.size() != expected.length) {
            return false;
        }

        int index = 0;
        for (String element : path) {
            if (!expected[index++].equals(element)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasNoNamespace(XMLStreamReader reader) {
        String namespace = reader.getNamespaceURI();
        return namespace == null || namespace.isEmpty();
    }

    private static void validatePath(Path xmlPath) {
        if (xmlPath == null) {
            throw new EngineException(
                    ErrorCode.INVALID_FILE_PATH,
                    "The XML path cannot be null.");
        }
        if (!Files.isRegularFile(xmlPath)) {
            throw new EngineException(
                    ErrorCode.FILE_NOT_FOUND,
                    "The XML file does not exist or is not a regular file: " + xmlPath + ".");
        }
    }

    private static EngineException xmlError(String message) {
        return new EngineException(ErrorCode.XML_PARSE_ERROR, message);
    }

    private static EngineException xmlError(String message, Throwable cause) {
        return new EngineException(ErrorCode.XML_PARSE_ERROR, message, cause);
    }

    private static final class DetectionState {
        private boolean rootSeen;
        private boolean rootEvents;
        private boolean assignment1Marker;
        private boolean assignment2Marker;
        private XmlFormatVersion declaredVersion;
    }
}
