package guessmarket.engine.loading;

import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import guessmarket.engine.loading.jaxb.GuessMarket;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.xml.sax.SAXException;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.net.URL;
import java.nio.file.Path;


public final class XmlMarketLoader {

    private final JAXBContext context;
    private final Schema schema;

    public XmlMarketLoader() {
        try {
            context =
                    JAXBContext.newInstance(
                            GuessMarket.class);

            URL schemaUrl =
                    XmlMarketLoader.class.getResource(
                            "/GM-EX1-Schema.xsd");

            if (schemaUrl == null) {
                throw new IllegalStateException(
                        "Could not find "
                                + "GM-EX1-Schema.xsd "
                                + "on the classpath.");
            }

            SchemaFactory schemaFactory =
                    SchemaFactory.newInstance(
                            XMLConstants
                                    .W3C_XML_SCHEMA_NS_URI);

            /*
             * The XSD may not load other files
             * from the network or file system.
             */
            schemaFactory.setProperty(
                    XMLConstants.ACCESS_EXTERNAL_DTD,
                    "");

            schemaFactory.setProperty(
                    XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                    "");

            schema =
                    schemaFactory.newSchema(schemaUrl);

        } catch (JAXBException | SAXException ex) {
            throw new IllegalStateException(
                    "Could not initialize "
                            + "the JAXB XML loader.",
                    ex);
        }
    }

    public MarketDefinition load(Path xmlPath) {
        try {
            Unmarshaller unmarshaller =
                    context.createUnmarshaller();

            /*
             * Validate the XML against
             * GM-EX1-Schema.xsd.
             */
            unmarshaller.setSchema(schema);

            /*
             * Convert the XML file into
             * JAXB-generated Java objects.
             */
            GuessMarket xmlSystem =
                    (GuessMarket)
                            unmarshaller.unmarshal(
                                    xmlPath.toFile());

            return JaxbDefinitionMapper
                    .toDefinition(xmlSystem);

        } catch (JAXBException ex) {
            throw new EngineException(
                    ErrorCode.XML_PARSE_ERROR,
                    "Could not read or validate "
                            + "the XML file: "
                            + getXmlErrorMessage(ex),
                    ex);
        }
    }

    private static String getXmlErrorMessage(
            JAXBException exception) {

        Throwable linked =
                exception.getLinkedException();

        if (linked != null
                && linked.getMessage() != null) {
            return linked.getMessage();
        }

        if (exception.getMessage() != null) {
            return exception.getMessage();
        }

        return exception.getClass().getSimpleName();
    }
}