package com.appshala.emailnotificationservice.consumer;

import com.appshala.emailnotificationservice.event.GenericActivityEvent;
import com.appshala.emailnotificationservice.event.UserInvitedEvent;
import com.appshala.emailnotificationservice.serviceImpl.InAppNotificationService;
import com.appshala.emailnotificationservice.serviceImpl.MailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final InAppNotificationService inAppNotificationService;
    private final MailService mailService;


    @KafkaListener(
            topics = "${kafka.topics.user-invitation}",
            groupId="${spring.kafka.consumer.group-id}"
    )
    public void handleUserInvitation(UserInvitedEvent event) throws MessagingException {
        log.info("KAFKA : Received User Invitation Event for email: {}",event.email());
        mailService.sendInvitationEmail(
                event.email(),
                event.name(),
                event.invitationToken()
        );
    }

    @KafkaListener(
            topics = "${kafka.topics.generic-activity}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleGenericActivity(GenericActivityEvent event)
    {
        log.info("KAFKA : Received Activity Event for user {} . Type: {}",event.recipientUserId(),event.activityType());
        inAppNotificationService.processGenericActivity(event);
    }
}
