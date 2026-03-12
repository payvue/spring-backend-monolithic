package com.payvue.app.infrastructure.persistence.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JsonMapConverterTest {

  private final JsonMapConverter converter = new JsonMapConverter();

  @Test
  @DisplayName("Map을 JSON 문자열로 변환한다")
  void convertsToDatabaseColumn() throws Exception {
    String json = converter.convertToDatabaseColumn(Map.of("key", "value"));

    Map<String, String> parsed = new ObjectMapper().readValue(json, Map.class);

    assertThat(parsed).containsEntry("key", "value");
  }

  @Test
  @DisplayName("null Map은 null로 변환된다")
  void convertsNullToDatabaseColumn() {
    String json = converter.convertToDatabaseColumn(null);

    assertThat(json).isNull();
  }

  @Test
  @DisplayName("JSON 문자열은 Map으로 역직렬화된다")
  void convertsToEntityAttribute() {
    Map<String, String> map = converter.convertToEntityAttribute("{\"key\":\"value\"}");

    assertThat(map).containsEntry("key", "value");
  }

  @Test
  @DisplayName("빈 문자열은 빈 Map으로 변환된다")
  void convertsBlankToEmptyMap() {
    Map<String, String> map = converter.convertToEntityAttribute(" ");

    assertThat(map).isEmpty();
  }

  @Test
  @DisplayName("null 문자열은 빈 Map으로 변환된다")
  void convertsNullToEmptyMap() {
    Map<String, String> map = converter.convertToEntityAttribute(null);

    assertThat(map).isEmpty();
  }

  @Test
  @DisplayName("잘못된 JSON은 예외를 던진다")
  void throwsOnInvalidJson() {
    assertThatThrownBy(() -> converter.convertToEntityAttribute("{invalid}"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("metadata 역직렬화");
  }
}
