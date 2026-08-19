package guessmarket.engine.loading;

import guessmarket.engine.enums.CommissionType;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Parses XML into temporary definitions; it never mutates the live engine state. */
public final class XmlMarketLoader {
    public MarketDefinition load(Path xmlPath) {
        try {
            DocumentBuilderFactory factory = secureFactory();
            Document document = factory.newDocumentBuilder().parse(xmlPath.toFile());
            Element root = document.getDocumentElement();
            Element eventsElement = requiredDirectChild(root, "GM-events");

            List<EventDefinition> definitions = new ArrayList<>();
            for (Element eventElement : directChildren(eventsElement, "GM-event")) {
                definitions.add(parseEvent(eventElement));
            }
            return new MarketDefinition(definitions);
        } catch (EngineException ex) {
            throw ex;
        } catch (ParserConfigurationException | SAXException | IOException | RuntimeException ex) {
            throw new EngineException(
                    ErrorCode.XML_PARSE_ERROR,
                    "Could not read the XML file: " + ex.getMessage(),
                    ex);
        }
    }

    private static EventDefinition parseEvent(Element eventElement) {
        String name = eventElement.getAttribute("name").trim();
        int id = parseInt(requiredText(eventElement, "id"), "id");
        String description = requiredText(eventElement, "description");

        Element commissionElement = requiredDirectChild(eventElement, "commission");
        int commission = parseInt(commissionElement.getTextContent().trim(), "commission");
        CommissionType commissionType = CommissionType.fromXml(
                commissionElement.getAttribute("type"));

        Element optionsElement = requiredDirectChild(eventElement, "GM-options");
        List<String> optionNames = new ArrayList<>();
        for (Element optionElement : directChildren(optionsElement, "GM-option")) {
            optionNames.add(optionElement.getTextContent().trim());
        }

        Element methodElement = requiredDirectChild(eventElement, "GM-method");
        Element lmsrElement = requiredDirectChild(methodElement, "GM-LMSR");
        int b = parseInt(requiredText(lmsrElement, "b"), "b");

        return new EventDefinition(
                id, name, description, commission, commissionType, optionNames, b);
    }

    private static DocumentBuilderFactory secureFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private static String requiredText(Element parent, String tagName) {
        return requiredDirectChild(parent, tagName).getTextContent().trim();
    }

    private static Element requiredDirectChild(Element parent, String tagName) {
        List<Element> matches = directChildren(parent, tagName);
        if (matches.size() != 1) {
            throw new EngineException(
                    ErrorCode.INVALID_EVENT_DEFINITION,
                    "Expected exactly one <" + tagName + "> inside <" + parent.getTagName() + ">.");
        }
        return matches.get(0);
    }

    private static List<Element> directChildren(Element parent, String tagName) {
        List<Element> result = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element element && element.getTagName().equals(tagName)) {
                result.add(element);
            }
        }
        return result;
    }

    private static int parseInt(String value, String fieldName) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new EngineException(
                    ErrorCode.INVALID_EVENT_DEFINITION,
                    "The <" + fieldName + "> value must be a whole number: '" + value + "'.",
                    ex);
        }
    }
}