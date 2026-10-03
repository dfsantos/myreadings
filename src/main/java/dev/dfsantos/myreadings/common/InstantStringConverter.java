package dev.dfsantos.myreadings.common;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;

/**
 * Converte {@link Instant} <-> {@link String} (ISO-8601) para colunas TEXT, já que o
 * SQLite não possui um tipo de timestamp nativo e o mapeamento padrão do Hibernate
 * para Instant espera uma coluna TIMESTAMP.
 */
@Converter(autoApply = false)
public class InstantStringConverter implements AttributeConverter<Instant, String> {

    @Override
    public String convertToDatabaseColumn(Instant attribute) {
        return attribute == null ? null : attribute.toString();
    }

    @Override
    public Instant convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Instant.parse(dbData);
    }
}
