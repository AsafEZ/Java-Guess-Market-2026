package guessmarket.engine.loading;

import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import guessmarket.engine.loading.jaxb.v2.GuessMarket;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

final class Assignment2XmlUnmarshaller {
    private static final String SCHEMA_RESOURCE = "/GM-EX2-Schema.xsd";

    private final JAXBContext context;
    private final Schema schema;

    Assignment2XmlUnmarshaller() {
        try {
            context = JAXBContext.newInstance(GuessMarket.class);
            schema = loadSchema();
        } catch (JAXBException | SAXException | IOException ex) {
            throw new IllegalStateException(
                    "Could not initialize the Assignment 2 JAXB XML loader.",
                    ex);
        }
    }

    GuessMarket unmarshal(Path xmlPath) {
        validatePath(xmlPath);

        try (InputStream stream = Files.newInputStream(xmlPath)) {
            XMLInputFactory inputFactory = SecureXmlInputFactory.create();
            XMLStreamReader reader = inputFactory.createXMLStreamReader(stream);
            try {
                moveToRootElement(reader);
                Unmarshaller unmarshaller = context.createUnmarshaller();
                unmarshaller.setSchema(schema);
                return unmarshaller.unmarshal(reader, GuessMarket.class).getValue();
            } finally {
                reader.close();
            }
        } catch (EngineException ex) {
            throw ex;
        } catch (IOException | JAXBException | XMLStreamException ex) {
            throw new EngineException(
                    ErrorCode.XML_PARSE_ERROR,
                    "Could not read or validate the Assignment 2 XML file: "
                            + xmlErrorMessage(ex),
                    ex);
        }
    }

    private static Schema loadSchema() throws SAXException, IOException {
        URL schemaUrl = Assignment2XmlUnmarshaller.class.getResource(SCHEMA_RESOURCE);
        if (schemaUrl == null) {
            throw new IllegalStateException(
                    "Could not find GM-EX2-Schema.xsd on the classpath.");
        }

        SchemaFactory factory = SchemaFactory.newInstance(
                XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        try (InputStream stream = schemaUrl.openStream()) {
            StreamSource source = new StreamSource(stream);
            source.setSystemId(schemaUrl.toExternalForm());
            return factory.newSchema(source);
        }
    }

    private static void moveToRootElement(XMLStreamReader reader)
            throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.DTD) {
                throw new EngineException(
                        ErrorCode.XML_PARSE_ERROR,
                        "DOCTYPE declarations are not allowed.");
            }
            if (event == XMLStreamConstants.ENTITY_REFERENCE) {
                throw new EngineException(
                        ErrorCode.XML_PARSE_ERROR,
                        "Entity references are not allowed.");
            }
            if (event == XMLStreamConstants.START_ELEMENT) {
                return;
            }
        }
        throw new EngineException(
                ErrorCode.XML_PARSE_ERROR,
                "The XML document does not contain a root element.");
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

    private static String xmlErrorMessage(Exception exception) {
        if (exception instanceof JAXBException jaxbException
                && jaxbException.getLinkedException() != null
                && jaxbException.getLinkedException().getMessage() != null) {
            return jaxbException.getLinkedException().getMessage();
        }
        if (exception.getMessage() != null) {
            return exception.getMessage();
        }
        return exception.getClass().getSimpleName();
    }
}
