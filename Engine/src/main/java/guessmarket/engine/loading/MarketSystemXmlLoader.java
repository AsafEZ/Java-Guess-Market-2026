package guessmarket.engine.loading;

import guessmarket.engine.calculation.LmsrCalculator;
import guessmarket.engine.domain.MarketSystem;

import java.nio.file.Path;
import java.util.Objects;

public final class MarketSystemXmlLoader {
    private final XmlFormatDetector formatDetector = new XmlFormatDetector();
    private final XmlMarketLoader assignment1Loader = new XmlMarketLoader();
    private final MarketDefinitionValidator assignment1Validator =
            new MarketDefinitionValidator();
    private final MarketSystemFactory assignment1Factory;
    private final Assignment2XmlUnmarshaller assignment2Unmarshaller =
            new Assignment2XmlUnmarshaller();
    private final Assignment2JaxbDefinitionMapper assignment2Mapper =
            new Assignment2JaxbDefinitionMapper();
    private final Assignment2DefinitionValidator assignment2Validator =
            new Assignment2DefinitionValidator();
    private final Assignment2MarketSystemFactory assignment2Factory;

    public MarketSystemXmlLoader(LmsrCalculator calculator) {
        Objects.requireNonNull(calculator, "calculator");
        assignment1Factory = new MarketSystemFactory(calculator);
        assignment2Factory = new Assignment2MarketSystemFactory(calculator);
    }

    public LoadedSystem load(Path xmlPath) {
        return switch (formatDetector.detect(xmlPath)) {
            case ASSIGNMENT_1 -> loadAssignment1(xmlPath);
            case ASSIGNMENT_2 -> loadAssignment2(xmlPath);
        };
    }

    private LoadedSystem loadAssignment1(Path xmlPath) {
        MarketDefinition definition = assignment1Loader.load(xmlPath);
        assignment1Validator.validate(definition);
        MarketSystem system = assignment1Factory.create(definition);
        return new LoadedSystem(system, system.totalInitialSubsidy());
    }

    private LoadedSystem loadAssignment2(Path xmlPath) {
        Assignment2Definition definition = assignment2Mapper.map(
                assignment2Unmarshaller.unmarshal(xmlPath));
        assignment2Validator.validate(definition);
        Assignment2MarketSystemFactory.CreationResult result =
                assignment2Factory.create(definition);
        return new LoadedSystem(result.system(), result.totalInitialSubsidy());
    }

    public record LoadedSystem(
            MarketSystem system,
            double totalInitialSubsidy) {
        public LoadedSystem {
            Objects.requireNonNull(system, "system");
        }
    }
}
