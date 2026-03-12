package com.payvue.app.application.community.create;

import java.util.List;

public record CommunityCreateCommand(
    String name,
    String description,
    Long initialAmount,
    List<String> invitedMembers
) {}
