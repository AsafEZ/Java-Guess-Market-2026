package guessmarket.engine.loading;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

final class SecureXmlInputFactory {
    private SecureXmlInputFactory() {
    }

    static XMLInputFactory create() {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
        if (factory.isPropertySupported(XMLConstants.ACCESS_EXTERNAL_DTD)) {
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        }
        factory.setXMLResolver((publicId, systemId, baseUri, namespace) -> {
            throw new XMLStreamException("External XML resources are disabled.");
        });
        return factory;
    }
}
