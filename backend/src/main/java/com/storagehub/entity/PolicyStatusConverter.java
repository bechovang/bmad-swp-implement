package com.storagehub.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PolicyStatusConverter implements AttributeConverter<PolicyStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(PolicyStatus attribute) {
        return attribute == null ? null : attribute.dbValue();
    }

    @Override
    public PolicyStatus convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : PolicyStatus.fromDbValue(dbData);
    }
}
