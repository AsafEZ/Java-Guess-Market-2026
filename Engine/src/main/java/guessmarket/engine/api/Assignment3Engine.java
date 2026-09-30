package guessmarket.engine.api;

import guessmarket.engine.dto.UserSummary;
import guessmarket.engine.dto.AccountActivityDetails;

import java.util.List;

public interface Assignment3Engine extends GuessMarketEngine {
    UserSummary creditAccount(String userName, double amount);

    List<AccountActivityDetails> getAccountHistory(String userName);
}
