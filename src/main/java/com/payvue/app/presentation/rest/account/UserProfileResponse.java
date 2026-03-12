package com.payvue.app.presentation.rest.account;

record UserProfileResponse(
    String id,
    String name,
    String email,
    String avatar,
    String status
) {}
