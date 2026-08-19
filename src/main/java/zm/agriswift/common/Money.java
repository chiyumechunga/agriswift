package zm.agriswift.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money (BigDecimal amount, String currencyCode){

    public Money {
        Objects.requireNonNull(amount, "Amount must not be null");
        Objects.requireNonNull(currencyCode, "currencyCode must not be null");
         if (amount.scale() > 4){
             amount = amount.setScale(4, RoundingMode.HALF_UP);
         }
    }

    public static Money zmw(BigDecimal amount){
        return new Money(amount, "ZMW");
    }

    public Money add(Money other){
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currencyCode);
    }

    private void requireSameCurrency(Money other) {
        if (!currencyCode.equals(other.currencyCode)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: %s vs %s".formatted(currencyCode, other.currencyCode));
        }
    }
}
