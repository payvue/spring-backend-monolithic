package com.payvue.app.infrastructure.persistence.transaction;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payvue.core.transaction.command.meta.TransactionMeta;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Map;

@Converter(autoApply = false)
class TransactionMetaConverter implements AttributeConverter<TransactionMeta, String> {

  private static final ObjectMapper mapper = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(TransactionMeta attribute) {
    if (attribute == null) {
      return null;
    }
    try {
      return mapper.writeValueAsString(attribute.toRawMap());
    } catch (Exception ex) {
      throw new IllegalArgumentException("meta 직렬화에 실패했습니다.", ex);
    }
  }

  @Override
  public TransactionMeta convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) {
      return TransactionMeta.empty();
    }
    try {
      Map<String, String> raw = mapper.readValue(dbData, new TypeReference<>() {});
      return TransactionMeta.fromRawMap(raw);
    } catch (Exception ex) {
      throw new IllegalArgumentException("meta 역직렬화에 실패했습니다.", ex);
    }
  }
}
