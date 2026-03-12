package com.payvue.app.presentation.rest.account;

import io.swagger.v3.oas.annotations.media.Schema;

record AccountEditRequest(
    @Schema(description = "이름", example = "홍길동")
    String name,
    @Schema(description = "연락처", example = "01012341234")
    String contact
) {}
