package com.taxrecordsportal.tax_records_portal_backend.common_domain.email;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.TrackingSettings;
import com.sendgrid.helpers.mail.objects.ClickTrackingSetting;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.util.List;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMMM d, yyyy");
    private static final DecimalFormat AMOUNT_FMT = new DecimalFormat("#,##0.00");

    @Async
    public void sendInvoiceNotification(String to, String invoiceNumber, LocalDate invoiceDate,
                                         LocalDate dueDate, String description,
                                         BigDecimal amountDue, List<Attachments> attachments) {
        try {
            Context context = new Context();
            context.setVariable("invoiceNumber", invoiceNumber);
            context.setVariable("invoiceDate", invoiceDate.format(DATE_FMT));
            context.setVariable("dueDate", dueDate.format(DATE_FMT));
            context.setVariable("description", description);
            context.setVariable("amountDue", AMOUNT_FMT.format(amountDue));
            context.setVariable("portalLink", frontendUrl);

            String body = templateEngine.process("email/invoice-notification", context);
            sendHtmlEmail(to, "Invoice " + invoiceNumber + " — Upturn Tax Records Portal", body, attachments);
        } catch (Exception e) {
            log.error("Failed to send invoice email to {}: {}", to, e.getMessage(), e);
        }
    }

    @Async
    public void sendPaymentReceiptNotification(String to, String invoiceNumber, LocalDate paymentDate,
                                                BigDecimal amountPaid, BigDecimal remainingBalance,
                                                BigDecimal totalAmountDue) {
        try {
            Context context = new Context();
            context.setVariable("invoiceNumber", invoiceNumber);
            context.setVariable("paymentDate", paymentDate.format(DATE_FMT));
            context.setVariable("amountPaid", AMOUNT_FMT.format(amountPaid));
            context.setVariable("remainingBalance", AMOUNT_FMT.format(remainingBalance));
            context.setVariable("totalAmountDue", AMOUNT_FMT.format(totalAmountDue));
            context.setVariable("portalLink", frontendUrl);

            String body = templateEngine.process("email/payment-receipt", context);
            sendHtmlEmail(to, "Payment Receipt for Invoice " + invoiceNumber + " — Upturn Tax Records Portal", body);
        } catch (Exception e) {
            log.error("Failed to send payment receipt email to {}: {}", to, e.getMessage(), e);
        }
    }

    @Async
    public void sendEndOfEngagementLetterEmail(String to, String subject, String renderedHtmlBody) {
        try {
            Context context = new Context();
            context.setVariable("body", renderedHtmlBody);

            String body = templateEngine.process("email/engagement-letter", context);
            sendHtmlEmail(to, subject, body);
        } catch (Exception e) {
            log.error("Failed to send engagement letter email to {}: {}", to, e.getMessage(), e);
        }
    }

    private void sendHtmlEmail(String to, String subject, String body) {
        sendHtmlEmail(to, subject, body, null);
    }

    private void sendHtmlEmail(String to, String subject, String body, List<Attachments> attachments) {
        try {
            log.info("Sending email to {} via SendGrid", to);

            Email from = new Email(fromEmail);
            Email toEmail = new Email(to);
            Content content = new Content("text/html", body);
            Mail mail = new Mail(from, subject, toEmail, content);

            if (attachments != null) {
                for (Attachments a : attachments) {
                    mail.addAttachments(a);
                }
            }

            ClickTrackingSetting clickTracking = new ClickTrackingSetting();
            clickTracking.setEnable(false);
            clickTracking.setEnableText(false);
            TrackingSettings trackingSettings = new TrackingSettings();
            trackingSettings.setClickTrackingSetting(clickTracking);
            mail.setTrackingSettings(trackingSettings);

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);

            if (response.getStatusCode() >= 400) {
                log.error("SendGrid error sending to {}: status={}", to, response.getStatusCode());
                throw new RuntimeException("Failed to send email via SendGrid, status=" + response.getStatusCode());
            }

            log.info("Email sent successfully to {} (status={})", to, response.getStatusCode());
        } catch (IOException e) {
            log.error("IOException sending email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
