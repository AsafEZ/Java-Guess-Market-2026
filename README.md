# Java Guess Market 2026

Guess Market includes three independent Maven modules:

- `Engine`: Assignment 1 and Assignment 2 domain, XML loading, LMSR, and Order Book logic.
- `ConsoleUI`: the Assignment 1 console application.
- `JavaFXUI`: the Assignment 2 desktop application.

## Requirements

- JDK 25
- Maven 3.9 or later

JavaFX and JAXB are downloaded by Maven. Do not configure a local JavaFX SDK or add library paths in the IDE.

## Build And Test

Run the modules in dependency order from the repository root:

```powershell
cd Engine
mvn clean install

cd ../ConsoleUI
mvn clean package

cd ../JavaFXUI
mvn clean package
```

The generated JARs are:

```text
Engine/target/Engine-1.0-SNAPSHOT.jar
ConsoleUI/target/consoleUI-1.0-SNAPSHOT.jar
JavaFXUI/target/JavaFXUI-1.0-SNAPSHOT.jar
```

## Run The JavaFX Application

Install Engine first, then launch JavaFXUI through its Maven plugin:

```powershell
cd Engine
mvn install

cd ../JavaFXUI
mvn javafx:run
```

In the application, select `Load XML` and choose an Assignment 1 or Assignment 2 XML file. The current file path is display-only and updates after a successful load. Assignment 2 events begin as `Not Started`; select the assigned Market Maker and event in the Users tab to open or close it. LMSR purchases and Order Book BUY/SELL orders are also submitted from the selected user's event area.

The official Assignment 2 examples used by the automated tests are stored under:

```text
Engine/src/test/resources/assignment2/xml
```

## Run The Engine Smoke Test

After `mvn test` in `Engine`, build the dependency classpath and run the smoke test with assertions enabled:

```powershell
cd Engine
mvn dependency:build-classpath "-Dmdep.outputFile=target/smoke-classpath.txt"
$dependencies = Get-Content target/smoke-classpath.txt -Raw
java -ea -cp "target/test-classes;target/classes;$dependencies" guessmarket.engine.EngineSmokeTest
```
