package guessmarket.engine.api;

import guessmarket.engine.dto.UserSummary;

public interface Assignment3Engine extends GuessMarketEngine {
    UserSummary creditAccount(String userName, double amount);
}
