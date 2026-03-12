package com.payvue.app.application.auth.recovery;

public record FindPasswordCommand(String username, String email) {}
