package guessmarket.engine.loading;

import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import guessmarket.engine.loading.jaxb.v3.GuessMarket;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

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
import org.xml.sax.SAXException;

final class Assignment3XmlUnmarshaller {
    private static final String SCHEMA_RESOURCE = "/GM-EX3-Schema.xsd";

    private final JAXBContext context;
    private final Schema schema;

    Assignment3XmlUnmarshaller() {
        try {
            context = JAXBContext.newInstance(GuessMarket.class);
            schema = loadSchema();
        } catch (JAXBException | SAXException | IOException exception) {
            throw new IllegalStateException("Could not initialize the Assignment 3 XML loader.",
                    exception);
        }
    }

    GuessMarket unmarshal(InputStream stream) {
        if (stream == null) {
            throw new EngineException(ErrorCode.XML_PARSE_ERROR,
                    "The uploaded XML stream is missing.");
        }
        try {
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
        } catch (EngineException exception) {
            throw exception;
        } catch (JAXBException | XMLStreamException exception) {
            throw new EngineException(ErrorCode.XML_PARSE_ERROR,
                    "Could not read or validate the Assignment 3 XML: "
                            + xmlErrorMessage(exception), exception);
        }
    }

    private static Schema loadSchema() throws SAXException, IOException {
        URL schemaUrl = Assignment3XmlUnmarshaller.class.getResource(SCHEMA_RESOURCE);
        if (schemaUrl == null) {
            throw new IllegalStateException("Could not find GM-EX3-Schema.xsd on the classpath.");
        }
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
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
            if (event == XMLStreamConstants.DTD || event == XMLStreamConstants.ENTITY_REFERENCE) {
                throw new EngineException(ErrorCode.XML_PARSE_ERROR,
                        "DOCTYPE declarations and entity references are not allowed.");
            }
            if (event == XMLStreamConstants.START_ELEMENT) {
                return;
            }
        }
        throw new EngineException(ErrorCode.XML_PARSE_ERROR,
                "The uploaded XML does not contain a root element.");
    }

    private static String xmlErrorMessage(Exception exception) {
        if (exception instanceof JAXBException jaxb
                && jaxb.getLinkedException() != null
                && jaxb.getLinkedException().getMessage() != null) {
            return jaxb.getLinkedException().getMessage();
        }
        return exception.getMessage() != null
                ? exception.getMessage() : exception.getClass().getSimpleName();
    }
}
