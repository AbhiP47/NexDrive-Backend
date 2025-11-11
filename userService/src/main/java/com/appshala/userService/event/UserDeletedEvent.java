package com.appshala.userService.event;

import java.util.UUID;

public record UserDeletedEvent (
    UUID userId,
    String timestamp
){}

