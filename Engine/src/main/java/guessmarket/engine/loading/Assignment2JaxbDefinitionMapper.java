package guessmarket.engine.loading;

import guessmarket.engine.enums.CommissionType;
import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;
import guessmarket.engine.loading.jaxb.v2.Commission;
import guessmarket.engine.loading.jaxb.v2.GMEvent;
import guessmarket.engine.loading.jaxb.v2.GMMarketMaker;
import guessmarket.engine.loading.jaxb.v2.GMMethod;
import guessmarket.engine.loading.jaxb.v2.GMOrderBook;
import guessmarket.engine.loading.jaxb.v2.GMUser;
import guessmarket.engine.loading.jaxb.v2.GuessMarket;

import java.util.List;

final class Assignment2JaxbDefinitionMapper {

    Assignment2Definition map(GuessMarket source) {
        GuessMarket root = requirePart(source, "Guess-Market root");

        List<UserDefinition> users = requirePart(root.getGMUsers(), "GM-users")
                .getGMUser()
                .stream()
                .map(this::mapUser)
                .toList();
        List<Assignment2EventDefinition> events = requirePart(
                root.getGMEvents(),
                "GM-events")
                .getGMEvent()
                .stream()
                .map(this::mapEvent)
                .toList();

        return new Assignment2Definition(users, events);
    }

    private UserDefinition mapUser(GMUser source) {
        GMUser user = requirePart(source, "GM-user");
        GMMarketMaker marketMaker = user.getGMMarketMaker();
        List<Integer> marketMakerEventIds = marketMaker == null
                ? List.of()
                : marketMaker.getEvent().stream()
                        .map(guessmarket.engine.loading.jaxb.v2.Event::getId)
                        .toList();

        return new UserDefinition(
                requirePart(user.getName(), "GM-user name"),
                user.getInitialCash(),
                marketMakerEventIds);
    }

    private Assignment2EventDefinition mapEvent(GMEvent source) {
        GMEvent event = requirePart(source, "GM-event");
        List<OptionDefinition> options = requirePart(
                event.getGMOptions(),
                "GM-options")
                .getGMOption()
                .stream()
                .map(name -> new OptionDefinition(
                        requirePart(name, "GM-option value")))
                .toList();

        return new Assignment2EventDefinition(
                event.getId(),
                requirePart(event.getName(), "GM-event name"),
                requirePart(event.getDescription(), "description"),
                mapCommission(event.getCommission()),
                options,
                mapTradingMechanism(event.getGMMethod()));
    }

    private CommissionDefinition mapCommission(Commission source) {
        Commission commission = requirePart(source, "commission");
        CommissionType type = switch (requirePart(
                commission.getType(),
                "commission type")) {
            case "on-purchase" -> CommissionType.ON_PURCHASE;
            case "on-close" -> CommissionType.ON_CLOSE;
            default -> throw mappingError("Unexpected commission type.");
        };
        return new CommissionDefinition(type, commission.getValue());
    }

    private TradingMechanismDefinition mapTradingMechanism(GMMethod source) {
        GMMethod method = requirePart(source, "GM-method");
        boolean hasLmsr = method.getGMLMSR() != null;
        boolean hasOrderBook = method.getGMOrderBook() != null;
        if (hasLmsr == hasOrderBook) {
            throw mappingError(
                    "GM-method must contain exactly one trading mechanism.");
        }
        if (hasLmsr) {
            return new LmsrDefinition(method.getGMLMSR().getB());
        }

        GMOrderBook orderBook = method.getGMOrderBook();
        boolean allowMint = switch (requirePart(
                orderBook.getAllowMint(),
                "GM-order-book allow-mint")) {
            case "true" -> true;
            case "false" -> false;
            default -> throw mappingError("Unexpected allow-mint value.");
        };
        return new OrderBookDefinition(
                allowMint,
                orderBook.getInitial(),
                orderBook.getD());
    }

    private static <T> T requirePart(T value, String partName) {
        if (value == null) {
            throw mappingError("Missing XML structure: " + partName + ".");
        }
        return value;
    }

    private static EngineException mappingError(String message) {
        return new EngineException(ErrorCode.XML_PARSE_ERROR, message);
    }
}
