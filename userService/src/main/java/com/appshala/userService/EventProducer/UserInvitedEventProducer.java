package com.appshala.userService.EventProducer;


import com.appshala.userService.event.UserInvitedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserInvitedEventProducer {
    @Value("${kafka.topics.user-invitation}")
    private  String USER_INVITATION_TOPIC ;

    private final KafkaTemplate<String,Object> kafkaTemplate;

    public void publishUserInvitedEvent(UserInvitedEvent event)
    {
        log.info("KAFKA : Publishing UserInvitedEvent for eamil: {}",event.email());

        kafkaTemplate.send(USER_INVITATION_TOPIC , event.userId() , event)
                .whenComplete((result , ex )-> {
                    if(ex==null)
                    {
                        log.info("KAFKA : Successfully sent event to partition {} with offset {}",
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                    else{
                        log.error("KAFKA : Failed to send UserInvitedEvent for {}: {}",event.email(),ex.getMessage());
                    }
                });
    }
}
