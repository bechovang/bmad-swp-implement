package com.storagehub.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps the {@code users.Status} TINYINT column onto {@link UserStatus}
 * (0=inactive, 1=active, 2=locked - V1 DDL comment). The relational side is
 * Byte precisely because the column is TINYINT: ddl-auto: validate derives
 * the expected JDBC type from the converter, and an Integer-side converter
 * would demand INTEGER and fail boot. An out-of-domain code surfaces as an
 * IllegalArgumentException on load rather than a silent default, because
 * account status gates authentication.
 */
@Converter
public class UserStatusConverter implements AttributeConverter<UserStatus, Byte> {

    @Override
    public Byte convertToDatabaseColumn(UserStatus attribute) {
        return attribute == null ? null : (byte) attribute.dbValue();
    }

    @Override
    public UserStatus convertToEntityAttribute(Byte dbData) {
        return dbData == null ? null : UserStatus.fromDbValue(dbData);
    }
}
