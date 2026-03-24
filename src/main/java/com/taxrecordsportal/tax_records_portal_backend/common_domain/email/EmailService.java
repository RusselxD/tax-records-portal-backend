package com.taxrecordsportal.tax_records_portal_backend.common_domain.email;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;

@Slf4j
@Service
public class EmailService {

    private final TemplateEngine templateEngine;
    private final SendGrid sendGrid;
    private final String fromEmail;
    private final String frontendUrl;

    public EmailService(
            TemplateEngine templateEngine,
            @Value("${sendgrid.api-key}") String apiKey,
            @Value("${spring.mail.from}") String fromEmail,
            @Value("${application.frontend-url}") String frontendUrl
    ) {
        this.templateEngine = templateEngine;
        this.sendGrid = new SendGrid(apiKey);
        this.fromEmail = fromEmail;
        this.frontendUrl = frontendUrl;
    }

    @PostConstruct
    void logMailConfig() {
        log.info("SendGrid email configured with from={}", fromEmail);
    }

    @Async
    public void sendActivationEmail(String to, String firstName, String token) {
        try {
            String activationLink = frontendUrl + "/auth/activate-account/" + token;

            Context context = new Context();
            context.setVariable("firstName", firstName);
            context.setVariable("activationLink", activationLink);

            String body = templateEngine.process("email/account-activation", context);
            sendHtmlEmail(to, "Activate Your Account", body);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    @Async
    public void sendPasswordResetEmail(String to, String firstName, String token) {
        try {
            String resetLink = frontendUrl + "/auth/reset-password/" + token;

            Context context = new Context();
            context.setVariable("firstName", firstName);
            context.setVariable("resetLink", resetLink);

            String body = templateEngine.process("email/password-reset", context);
            sendHtmlEmail(to, "Reset Your Password", body);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    private void sendHtmlEmail(String to, String subject, String body) {
        try {
            log.info("Sending email to {} via SendGrid", to);

            Email from = new Email(fromEmail);
            Email toEmail = new Email(to);
            Content content = new Content("text/html", body);
            Mail mail = new Mail(from, subject, toEmail, content);

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);

            if (response.getStatusCode() >= 400) {
                log.error("SendGrid error sending to {}: status={}, body={}", to, response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to send email: " + response.getBody());
            }

            log.info("Email sent successfully to {} (status={})", to, response.getStatusCode());
        } catch (IOException e) {
            log.error("IOException sending email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
