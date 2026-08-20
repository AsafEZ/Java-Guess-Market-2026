package guessmarket.engine.loading;

import guessmarket.engine.enums.CommissionType;
import guessmarket.engine.loading.jaxb.Comision;
import guessmarket.engine.loading.jaxb.GMEvent;
import guessmarket.engine.loading.jaxb.GuessMarket;

import java.util.List;


public final class JaxbDefinitionMapper {

    private JaxbDefinitionMapper() {
    }

    public static MarketDefinition toDefinition(GuessMarket source) {

        List<EventDefinition> events = source.getGMEvents().getGMEvent().stream().map(JaxbDefinitionMapper::toEventDefinition).toList();

        return new MarketDefinition(events);
    }

    private static EventDefinition toEventDefinition(GMEvent source) {
        Comision comision = source.getComision();


        String eventName = String.join(" ", source.getName()).trim();

        List<String> optionNames = source.getGMOptions().getGMOption().stream().map(String::trim).toList();

        int b =source.getGMMethod().getGMLMSR().getB();

        return new EventDefinition(
                source.getId(),
                eventName,
                source.getDescription().trim(),
                comision.getValue(),
                CommissionType.fromXml(
                        comision.getType()),
                optionNames,
                b);
    }
}