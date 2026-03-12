package com.payvue.app.infrastructure.persistence.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payvue.core.transaction.command.meta.TransactionMeta;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TransactionMetaConverterTest {

  private final TransactionMetaConverter converter = new TransactionMetaConverter();

  @Test
  @DisplayName("TransactionMeta를 JSON 문자열로 변환한다")
  void convertsToDatabaseColumn() {
    TransactionMeta meta = TransactionMeta.fromRawMap(Map.of("CLIENT_REQUEST_ID", "value"));

    String json = converter.convertToDatabaseColumn(meta);

    assertThat(json).contains("CLIENT_REQUEST_ID").contains("value");
  }

  @Test
  @DisplayName("null meta는 null로 변환된다")
  void convertsNullMetaToNull() {
    String json = converter.convertToDatabaseColumn(null);

    assertThat(json).isNull();
  }

  @Test
  @DisplayName("JSON 문자열은 TransactionMeta로 변환된다")
  void convertsToEntityAttribute() {
    TransactionMeta meta = converter.convertToEntityAttribute("{\"CLIENT_REQUEST_ID\":\"value\"}");

    assertThat(meta.toRawMap()).containsEntry("CLIENT_REQUEST_ID", "value");
  }

  @Test
  @DisplayName("null 문자열은 빈 meta로 변환된다")
  void convertsNullToEmptyMeta() {
    TransactionMeta meta = converter.convertToEntityAttribute(null);

    assertThat(meta.toRawMap()).isEmpty();
  }

  @Test
  @DisplayName("빈 문자열은 비어있는 meta로 변환된다")
  void convertsBlankToEmptyMeta() {
    TransactionMeta meta = converter.convertToEntityAttribute(" ");

    assertThat(meta.toRawMap()).isEmpty();
  }

  @Test
  @DisplayName("잘못된 JSON은 예외를 던진다")
  void throwsOnInvalidJson() {
    assertThatThrownBy(() -> converter.convertToEntityAttribute("{invalid}"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("meta 역직렬화");
  }
}
