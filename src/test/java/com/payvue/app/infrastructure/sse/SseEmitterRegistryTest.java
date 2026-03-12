package com.payvue.app.infrastructure.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SseEmitterRegistryTest {

  @Test
  @DisplayName("register는 emitter를 저장하고 get으로 조회된다")
  void registerStoresEmitter() {
    SseEmitterRegistry registry = new SseEmitterRegistry();

    SseEmitter emitter = registry.register("user-1");

    assertThat(registry.get("user-1")).isSameAs(emitter);
  }

  @Test
  @DisplayName("같은 사용자로 register를 호출하면 기존 emitter를 덮어쓴다")
  void registerOverridesExistingEmitter() {
    SseEmitterRegistry registry = new SseEmitterRegistry();
    SseEmitter first = registry.register("user-1");

    SseEmitter second = registry.register("user-1");

    assertThat(second).isNotSameAs(first);
    assertThat(registry.get("user-1")).isSameAs(second);
  }

  @Test
  @DisplayName("remove는 emitter를 제거한다")
  void removeDeletesEmitter() {
    SseEmitterRegistry registry = new SseEmitterRegistry();
    registry.register("user-1");

    registry.remove("user-1");

    assertThat(registry.get("user-1")).isNull();
  }

  @Test
  @DisplayName("complete 호출 시 emitter가 제거된다")
  void completeRemovesEmitter() {
    SseEmitterRegistry registry = new SseEmitterRegistry();
    SseEmitter emitter = registry.register("user-1");

    emitter.complete();

    assertThat(registry.get("user-1")).isNull();
  }

  @Test
  @DisplayName("completeWithError 호출 시 emitter가 제거된다")
  void completeWithErrorRemovesEmitter() {
    SseEmitterRegistry registry = new SseEmitterRegistry();
    SseEmitter emitter = registry.register("user-1");

    emitter.completeWithError(new IllegalStateException("boom"));

    assertThat(registry.get("user-1")).isNull();
  }

  @Test
  @DisplayName("timeout 콜백 실행 시 emitter가 제거된다")
  void timeoutCallbackRemovesEmitter() {
    SseEmitterRegistry registry = new SseEmitterRegistry();
    SseEmitter emitter = registry.register("user-1");

    Runnable timeoutCallback = findCallback(emitter, "timeoutCallback", Runnable.class);
    timeoutCallback.run();

    assertThat(registry.get("user-1")).isNull();
  }

  @Test
  @DisplayName("error 콜백 실행 시 emitter가 제거된다")
  void errorCallbackRemovesEmitter() {
    SseEmitterRegistry registry = new SseEmitterRegistry();
    SseEmitter emitter = registry.register("user-1");

    Consumer<Throwable> errorCallback = findCallback(emitter, "errorCallback", Consumer.class);
    errorCallback.accept(new RuntimeException("boom"));

    assertThat(registry.get("user-1")).isNull();
  }

  @Test
  @DisplayName("콜백 필드를 찾지 못하면 예외를 던진다")
  void throwsWhenCallbackFieldMissing() {
    SseEmitterRegistry registry = new SseEmitterRegistry();
    SseEmitter emitter = registry.register("user-1");

    assertThatThrownBy(() -> findCallback(emitter, "missingCallback", Runnable.class))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("missingCallback");
  }

  @SuppressWarnings("unchecked")
  private static <T> T findCallback(SseEmitter emitter, String fieldName, Class<T> type) {
    Field field = findField(emitter.getClass(), fieldName);
    try {
      field.setAccessible(true);
      Object value = field.get(emitter);
      if (value == null) {
        throw new IllegalStateException("callback is null: " + fieldName);
      }
      return (T) value;
    } catch (IllegalAccessException ex) {
      throw new IllegalStateException("cannot access callback: " + fieldName, ex);
    }
  }

  private static Field findField(Class<?> type, String name) {
    Class<?> current = type;
    while (current != null) {
      try {
        return current.getDeclaredField(name);
      } catch (NoSuchFieldException ignored) {
        current = current.getSuperclass();
      }
    }
    throw new IllegalStateException("missing field: " + name);
  }
}
