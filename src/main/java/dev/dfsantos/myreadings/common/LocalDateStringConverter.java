package dev.dfsantos.myreadings.common;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;

/**
 * Converte {@link LocalDate} <-> {@link String} (ISO-8601) para colunas TEXT, pelo mesmo
 * motivo de {@link InstantStringConverter}: o SQLite não possui um tipo de data nativo e o
 * {@code LocalDateJavaType} do Hibernate não sabe converter diretamente para {@code String}
 * ao vincular o parâmetro JDBC.
 */
@Converter(autoApply = false)
public class LocalDateStringConverter implements AttributeConverter<LocalDate, String> {

    @Override
    public String convertToDatabaseColumn(LocalDate attribute) {
        return attribute == null ? null : attribute.toString();
    }

    @Override
    public LocalDate convertToEntityAttribute(String dbData) {
        return dbData == null ? null : LocalDate.parse(dbData);
    }
}
