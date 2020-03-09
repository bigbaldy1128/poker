package com.bigbaldy.poker.model.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

public enum ThirdPartyUserType {
    UNKNOWN(0),
    WEIXIN(1);

    private Integer value;

    ThirdPartyUserType(int value) {
        this.value = value;
    }

    @JsonCreator
    public static ThirdPartyUserType of(int value) {
        for (ThirdPartyUserType verificationType : ThirdPartyUserType.values()) {
            if (verificationType.value == value) {
                return verificationType;
            }
        }
        return UNKNOWN;
    }

    @JsonValue
    public Integer getValue() {
        return this.value;
    }

    @Converter(autoApply = true)
    public final static class ThirdPartyTypeJpaConverter implements
            AttributeConverter<ThirdPartyUserType, Integer> {

        @Override
        public Integer convertToDatabaseColumn(ThirdPartyUserType thirdPartyUserType) {
            if (thirdPartyUserType == null) {
                return UNKNOWN.getValue();
            }
            return thirdPartyUserType.getValue();
        }

        @Override
        public ThirdPartyUserType convertToEntityAttribute(Integer value) {
            if (value == null) {
                return UNKNOWN;
            }
            return ThirdPartyUserType.of(value);
        }
    }
}
