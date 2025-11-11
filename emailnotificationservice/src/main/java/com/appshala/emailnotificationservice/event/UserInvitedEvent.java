package com.appshala.emailnotificationservice.event;

public record UserInvitedEvent(
        String userId,
        String name,
        String email,
        String invitationToken,
        String timeStamp
) {
}
