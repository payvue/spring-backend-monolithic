package com.payvue.app.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

class RestAuthenticationEntryPointTest {

  @Test
  @DisplayName("인증 실패 시 401과 UNAUTHORIZED 응답을 반환한다")
  void writesUnauthorizedResponse() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(mapper);
    MockHttpServletResponse response = new MockHttpServletResponse();

    entryPoint.commence(mockRequest(), response, new AuthenticationException("unauthorized") {});

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentType()).isEqualTo("application/json");
    JsonNode body = mapper.readTree(response.getContentAsString());
    assertThat(body.get("code").asText()).isEqualTo("UNAUTHORIZED");
  }

  private HttpServletRequest mockRequest() {
    return org.mockito.Mockito.mock(HttpServletRequest.class);
  }
}
