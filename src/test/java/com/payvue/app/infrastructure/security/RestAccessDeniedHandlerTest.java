package com.payvue.app.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class RestAccessDeniedHandlerTest {

  @Test
  @DisplayName("접근 거부 시 403과 FORBIDDEN 응답을 반환한다")
  void writesForbiddenResponse() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    RestAccessDeniedHandler handler = new RestAccessDeniedHandler(mapper);
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.handle(mockRequest(), response, new AccessDeniedException("denied"));

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentType()).isEqualTo("application/json");
    JsonNode body = mapper.readTree(response.getContentAsString());
    assertThat(body.get("code").asText()).isEqualTo("FORBIDDEN");
  }

  private HttpServletRequest mockRequest() {
    return org.mockito.Mockito.mock(HttpServletRequest.class);
  }
}
