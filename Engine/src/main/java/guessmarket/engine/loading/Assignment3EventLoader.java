package guessmarket.engine.loading;

import guessmarket.engine.calculation.LmsrCalculator;
import guessmarket.engine.domain.MarketEvent;
import guessmarket.engine.enums.CommissionType;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import guessmarket.engine.loading.jaxb.v3.Commission;
import guessmarket.engine.loading.jaxb.v3.GMEvent;
import guessmarket.engine.loading.jaxb.v3.GMMethod;
import guessmarket.engine.loading.jaxb.v3.GMOrderBook;
import guessmarket.engine.loading.jaxb.v3.GuessMarket;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class Assignment3EventLoader {
    private final Assignment3XmlUnmarshaller unmarshaller = new Assignment3XmlUnmarshaller();
    private final Assignment2MarketSystemFactory eventFactory;

    public Assignment3EventLoader(LmsrCalculator calculator) {
        eventFactory = new Assignment2MarketSystemFactory(calculator);
    }

    public List<MarketEvent> load(InputStream stream, int firstEventId) {
        GuessMarket root = requirePart(unmarshaller.unmarshal(stream), "Guess-Market");
        List<GMEvent> sources = requirePart(root.getGMEvents(), "GM-events").getGMEvent();
        if ((long) firstEventId + sources.size() - 1 > Integer.MAX_VALUE) {
            throw new EngineException(ErrorCode.ARITHMETIC_OVERFLOW,
                    "There are no event identifiers left for this upload.");
        }
        List<Assignment2EventDefinition> definitions = new ArrayList<>();
        for (int index = 0; index < sources.size(); index++) {
            definitions.add(mapEvent(sources.get(index), firstEventId + index));
        }
        Assignment2DefinitionValidator.validateEvents(
                new Assignment2Definition(List.of(), definitions));
        return definitions.stream().map(eventFactory::createEvent).toList();
    }

    private static Assignment2EventDefinition mapEvent(GMEvent source, int eventId) {
        GMEvent event = requirePart(source, "GM-event");
        List<OptionDefinition> options = requirePart(event.getGMOptions(), "GM-options")
                .getGMOption().stream()
                .map(name -> new OptionDefinition(requirePart(name, "GM-option")))
                .toList();
        return new Assignment2EventDefinition(
                eventId,
                requirePart(event.getName(), "GM-event name"),
                requirePart(event.getDescription(), "description"),
                mapCommission(event.getCommission()),
                options,
                mapMechanism(event.getGMMethod()));
    }

    private static CommissionDefinition mapCommission(Commission source) {
        Commission commission = requirePart(source, "commission");
        CommissionType type = switch (requirePart(commission.getType(), "commission type")) {
            case "on-purchase" -> CommissionType.ON_PURCHASE;
            case "on-close" -> CommissionType.ON_CLOSE;
            default -> throw mappingError("Unsupported commission type.");
        };
        return new CommissionDefinition(type, commission.getValue());
    }

    private static TradingMechanismDefinition mapMechanism(GMMethod source) {
        GMMethod method = requirePart(source, "GM-method");
        if ((method.getGMLMSR() == null) == (method.getGMOrderBook() == null)) {
            throw mappingError("GM-method must contain exactly one trading mechanism.");
        }
        if (method.getGMLMSR() != null) {
            return new LmsrDefinition(method.getGMLMSR().getB());
        }
        GMOrderBook orderBook = method.getGMOrderBook();
        boolean allowMint = switch (requirePart(orderBook.getAllowMint(), "allow-mint")) {
            case "true" -> true;
            case "false" -> false;
            default -> throw mappingError("Unsupported allow-mint value.");
        };
        return new OrderBookDefinition(allowMint, orderBook.getInitial(), orderBook.getD());
    }

    private static <T> T requirePart(T value, String name) {
        if (value == null) {
            throw mappingError("Missing XML structure: " + name + ".");
        }
        return value;
    }

    private static EngineException mappingError(String message) {
        return new EngineException(ErrorCode.XML_PARSE_ERROR, message);
    }
}
