package com.payvue.app.domain.auth;

import com.payvue.supporting.user.UserContextManager;
import java.util.Map;

record AuthContext(
    Authentication type,
    CompletionStrategyClassification completionStrategyClassification,
    UserContextManager userContextManager,
    SessionManager sessionManager,
    CompletionStrategy completionStrategy,
    Map<CompletionStrategyClassification.Sequence, AuthSequenceHandler> handlers
) {}
