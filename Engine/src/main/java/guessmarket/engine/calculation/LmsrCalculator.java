package guessmarket.engine.calculation;

/** Stateless and numerically stable LMSR calculations for two options. */
public final class LmsrCalculator {
    public double initialSubsidy(int b) {
        requirePositiveB(b);
        return b * Math.log(2.0);
    }

    public double optionValue(int b, long firstShares, long secondShares, int optionIndex) {
        requireState(b, firstShares, secondShares);
        if (optionIndex != 0 && optionIndex != 1) {
            throw new IllegalArgumentException("Option index must be 0 or 1.");
        }

        double delta = ((double) secondShares - (double) firstShares) / b;
        double firstValue;
        if (delta >= 0.0) {
            double expNegative = Math.exp(-delta);
            firstValue = expNegative / (1.0 + expNegative);
        } else {
            double expPositive = Math.exp(delta);
            firstValue = 1.0 / (1.0 + expPositive);
        }
        return optionIndex == 0 ? firstValue : 1.0 - firstValue;
    }

    public double cost(int b, long firstShares, long secondShares) {
        requireState(b, firstShares, secondShares);
        double firstScaled = (double) firstShares / b;
        double secondScaled = (double) secondShares / b;
        double maximum = Math.max(firstScaled, secondScaled);
        return b * (maximum + Math.log(
                Math.exp(firstScaled - maximum) + Math.exp(secondScaled - maximum)));
    }

    public double purchaseCost(
            int b,
            long firstShares,
            long secondShares,
            int optionIndex,
            long quantity) {
        requireState(b, firstShares, secondShares);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }
        if (optionIndex != 0 && optionIndex != 1) {
            throw new IllegalArgumentException("Option index must be 0 or 1.");
        }

        long newFirst = firstShares;
        long newSecond = secondShares;
        if (optionIndex == 0) {
            newFirst = Math.addExact(firstShares, quantity);
        } else {
            newSecond = Math.addExact(secondShares, quantity);
        }

        double result = cost(b, newFirst, newSecond) - cost(b, firstShares, secondShares);
        return result < 0.0 && result > -1.0e-9 ? 0.0 : result;
    }

    private static void requireState(int b, long firstShares, long secondShares) {
        requirePositiveB(b);
        if (firstShares < 0 || secondShares < 0) {
            throw new IllegalArgumentException("Share quantities cannot be negative.");
        }
    }

    private static void requirePositiveB(int b) {
        if (b <= 0) {
            throw new IllegalArgumentException("LMSR b must be positive.");
        }
    }
}
