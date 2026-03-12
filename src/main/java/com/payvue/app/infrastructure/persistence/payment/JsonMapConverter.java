package com.payvue.app.infrastructure.persistence.payment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Map;

@Converter(autoApply = false)
class JsonMapConverter implements AttributeConverter<Map<String, String>, String> {

  private static final ObjectMapper mapper = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(Map<String, String> attribute) {
    if (attribute == null) {
      return null;
    }
    try {
      return mapper.writeValueAsString(attribute);
    } catch (Exception ex) {
      throw new IllegalArgumentException("metadata 직렬화에 실패했습니다.", ex);
    }
  }

  @Override
  public Map<String, String> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) {
      return Map.of();
    }
    try {
      return mapper.readValue(dbData, new TypeReference<>() {});
    } catch (Exception ex) {
      throw new IllegalArgumentException("metadata 역직렬화에 실패했습니다.", ex);
    }
  }
}
