package com.payvue.app.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

class EmailTemplateRendererTest {

  @Test
  @DisplayName("렌더러는 템플릿 이름과 변수를 전달한다")
  void renderDelegatesToTemplateEngine() {
    TemplateEngine engine = mock(TemplateEngine.class);
    EmailTemplateRenderer renderer = new EmailTemplateRenderer(engine);

    when(engine.process(anyString(), any(Context.class)))
        .thenReturn("body");

    String body = renderer.render("alarm/verification", Map.of("key", "value"));

    ArgumentCaptor<Context> captor = ArgumentCaptor.forClass(Context.class);
    verify(engine).process(eq("alarm/verification"), captor.capture());

    assertThat(body).isEqualTo("body");
    assertThat(captor.getValue().getVariable("key")).isEqualTo("value");
  }
}
