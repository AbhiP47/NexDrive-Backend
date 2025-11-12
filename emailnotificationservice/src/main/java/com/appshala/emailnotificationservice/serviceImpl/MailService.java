package com.appshala.emailnotificationservice.serviceImpl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${app.frontend.invite-url}")
    private String frontendInviteUrl;

    public MailService(JavaMailSender mailSender)
    {
        this.mailSender = mailSender;
    }

    public void sendInvitationEmail(String toEmail , String userName , String token) throws RuntimeException, MessagingException {


        final String inviteLink = frontendInviteUrl + token;

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(toEmail);
            helper.setSubject("You've Been Invited to Join the AppShala Platform!");
            String htmlBody = String.format("""
                    <html>
                    <body style="font-family: Arial, sans-serif; line-height: 1.6;">
                        <h2>Hello %s,</h2>
                        <p>You have been invited to join the App Shala Drive platform. To activate your account and set your password, click the link below:</p>
                        <p style="padding: 12px; background-color: #f5f5f5; border-left: 5px solid #007bff; font-size: 1.1em;">
                            <a href="%s" style="color: #007bff; text-decoration: none; font-weight: bold;">Activate Account</a>
                        </p>
                        <p>If the button above doesn't work, copy and paste the following link into your web browser:</p>
                        <p><small>%s</small></p>
                        <br>
                        <p>If you have any questions, please contact support.</p>
                        <p>Thanks,<br>The App Shala Team</p>
                    </body>
                    </html>
                    """, userName, inviteLink, inviteLink);
            helper.setText(htmlBody, true);
            mailSender.send(mimeMessage);
            log.info("MAIL : Successfully sent the invitation mail to : {}", toEmail);
        } catch (MailException | jakarta.mail.MessagingException e) {
            log.error("MAIL ERROR: Failed to send invitation to {} ({}). Cause: {}", userName, toEmail, e.getMessage());
            throw new RuntimeException("Email dispatch failed, triggering  retry/DLQ", e);
        }
    }

}