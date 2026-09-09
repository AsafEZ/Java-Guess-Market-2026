package guessmarket.engine.trading.lmsr;

import guessmarket.engine.enums.TradingMethod;
import guessmarket.engine.trading.TradingMechanism;

public final class LmsrTradingMechanism implements TradingMechanism {

    private final int b;

    public LmsrTradingMechanism(int b) {
        if (b <= 0) {
            throw new IllegalArgumentException(
                    "LMSR parameter b must be positive"
            );
        }

        this.b = b;
    }

    @Override
    public TradingMethod getTradingMethod() {
        return TradingMethod.LMSR;
    }

    public int getB() {
        return b;
    }
}