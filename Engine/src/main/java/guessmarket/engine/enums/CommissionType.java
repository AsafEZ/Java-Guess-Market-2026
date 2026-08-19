package guessmarket.engine.enums;

import guessmarket.engine.exception.EngineException;
import guessmarket.engine.exception.ErrorCode;

import java.util.Locale;

public enum CommissionType {
    ON_PURCHASE("on-purchase"),
    ON_CLOSE("on-close");

    private final String xmlValue;

    CommissionType(String xmlValue) {
        this.xmlValue = xmlValue;
    }

    public String xmlValue() {
        return xmlValue;
    }

    public static CommissionType fromXml(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        for (CommissionType type : values()) {
            if (type.xmlValue.equals(normalized)) {
                return type;
            }
        }
        throw new EngineException(
                ErrorCode.UNKNOWN_COMMISSION_TYPE,
                "Unknown commission type: '" + value + "'. Expected on-purchase or on-close.");
    }
}