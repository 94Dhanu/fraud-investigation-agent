package com.fraudagent.persistence.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudagent.domain.model.PolicyCitation;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
public class PolicyCitationListJsonConverter implements AttributeConverter<List<PolicyCitation>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<PolicyCitation>> TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<PolicyCitation> attribute) {
        try {
            return attribute == null ? null : MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialise citations to JSON", e);
        }
    }

    @Override
    public List<PolicyCitation> convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? null : MAPPER.readValue(dbData, TYPE);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to deserialise JSON citations", e);
        }
    }
}
