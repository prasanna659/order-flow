package com.orderflow.notification.service;

import com.orderflow.notification.dto.OrderConfirmedEvent;
import com.orderflow.notification.dto.PasswordResetEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Sends transactional emails via Gmail SMTP.
 *
 * Key design choices:
 *
 * 1. The public send*() methods do guard-checks, log once, render the template,
 *    then delegate to a package-private doSend*() method.
 *
 * 2. Only the doSend*() methods are annotated @Retryable so the "EMAIL_SENDING"
 *    log and template rendering happen exactly once per event — not once per
 *    retry attempt.
 *
 * 3. sendHtml() wraps MessagingException as MailSendException (a MailException
 *    subclass) so every SMTP failure surfaces as MailException, which is already
 *    in the @Retryable retryFor list.  Previously MessagingException was wrapped
 *    in RuntimeException and @Retryable could not see it.
 *
 * 4. SMTP timeouts (10s) are set programmatically in MailConfig.java so they
 *    cannot be silently ignored by Spring Boot config binding.  Without them,
 *    a stalled connection hangs indefinitely and exhausts all retry attempts in
 *    a single network stall.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm z").withZone(ZoneId.of("UTC"));

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${notification.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender     = mailSender;
        this.templateEngine = templateEngine;
    }

    // ── Order Confirmed ──────────────────────────────────────────────────

    public void sendOrderConfirmed(OrderConfirmedEvent event) {
        if (!canSend(event.getOrderId(), event.getUserEmail())) return;
        log.info("EMAIL_SENDING type=ORDER_CONFIRMED orderId={} to={}", event.getOrderId(), event.getUserEmail());

        Context ctx = new Context();
        ctx.setVariable("orderId",     event.getOrderId());
        ctx.setVariable("username",    event.getUsername());
        ctx.setVariable("totalAmount", fmt(event.getTotalAmount()));
        ctx.setVariable("orderDate",
                DATE_FMT.format(event.getTimestamp() != null ? event.getTimestamp() : Instant.now()));
        ctx.setVariable("items",       event.getItems());
        ctx.setVariable("frontendUrl", frontendUrl);

        doSendOrderConfirmed(
                event.getUserEmail(),
                templateEngine.process("order-confirmed", ctx),
                event.getOrderId());
    }

    /**
     * Retried on transient SMTP failures. Backoff: 5s → 10s → 20s.
     * package-private so @Recover can be matched by Spring Retry's proxy.
     */
    @Retryable(retryFor = MailException.class, maxAttempts = 4,
               backoff = @Backoff(delay = 5000, multiplier = 2))
    void doSendOrderConfirmed(String to, String html, String orderId) {
        sendHtml(to, "Your OrderFlow order is confirmed ✓", html);
        log.info("EMAIL_SENT type=ORDER_CONFIRMED orderId={}", orderId);
    }

    @Recover
    void recoverOrderConfirmed(MailException e, String to, String html, String orderId) {
        log.error("EMAIL_FAILED type=ORDER_CONFIRMED orderId={} to={} — all retries exhausted, error={}",
                orderId, to, e.getMessage());
    }

    // ── Order Cancelled ──────────────────────────────────────────────────

    public void sendOrderCancelled(OrderConfirmedEvent event) {
        if (!canSend(event.getOrderId(), event.getUserEmail())) return;
        log.info("EMAIL_SENDING type=ORDER_CANCELLED orderId={} to={}", event.getOrderId(), event.getUserEmail());

        Context ctx = new Context();
        ctx.setVariable("orderId",       event.getOrderId());
        ctx.setVariable("username",      event.getUsername());
        ctx.setVariable("totalAmount",   fmt(event.getTotalAmount()));
        ctx.setVariable("failureReason", event.getFailureReason());
        ctx.setVariable("orderDate",
                DATE_FMT.format(event.getTimestamp() != null ? event.getTimestamp() : Instant.now()));
        ctx.setVariable("frontendUrl",   frontendUrl);

        doSendOrderCancelled(
                event.getUserEmail(),
                templateEngine.process("order-cancelled", ctx),
                event.getOrderId());
    }

    @Retryable(retryFor = MailException.class, maxAttempts = 4,
               backoff = @Backoff(delay = 5000, multiplier = 2))
    void doSendOrderCancelled(String to, String html, String orderId) {
        sendHtml(to, "Your OrderFlow order was cancelled", html);
        log.info("EMAIL_SENT type=ORDER_CANCELLED orderId={}", orderId);
    }

    @Recover
    void recoverOrderCancelled(MailException e, String to, String html, String orderId) {
        log.error("EMAIL_FAILED type=ORDER_CANCELLED orderId={} to={} — all retries exhausted, error={}",
                orderId, to, e.getMessage());
    }

    // ── Password Reset ───────────────────────────────────────────────────

    public void sendPasswordResetEmail(PasswordResetEvent event) {
        if (!mailEnabled || fromAddress.isBlank()) {
            log.info("EMAIL_SKIP type=PASSWORD_RESET reason=mail_not_configured userId={} resetLink={}",
                    event.getUserId(), event.getResetLink());
            return;
        }
        if (event.getUserEmail() == null || event.getUserEmail().isBlank()) {
            log.warn("EMAIL_SKIP type=PASSWORD_RESET reason=no_recipient userId={}", event.getUserId());
            return;
        }

        log.info("EMAIL_SENDING type=PASSWORD_RESET to={}", event.getUserEmail());

        Context ctx = new Context();
        ctx.setVariable("username",         event.getUsername());
        ctx.setVariable("resetLink",        event.getResetLink());
        ctx.setVariable("expiresInMinutes", event.getExpiresInMinutes());

        doSendPasswordReset(
                event.getUserEmail(),
                templateEngine.process("password-reset", ctx),
                event.getUserId());
    }

    @Retryable(retryFor = MailException.class, maxAttempts = 4,
               backoff = @Backoff(delay = 5000, multiplier = 2))
    void doSendPasswordReset(String to, String html, Long userId) {
        sendHtml(to, "Reset your OrderFlow password", html);
        log.info("EMAIL_SENT type=PASSWORD_RESET to={}", to);
    }

    @Recover
    void recoverPasswordReset(MailException e, String to, String html, Long userId) {
        log.error("EMAIL_FAILED type=PASSWORD_RESET userId={} to={} — all retries exhausted, error={}",
                userId, to, e.getMessage());
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private boolean canSend(String ref, String email) {
        if (!mailEnabled || fromAddress.isBlank()) {
            log.info("EMAIL_SKIP ref={} reason=mail_not_configured", ref);
            return false;
        }
        if (email == null || email.isBlank()) {
            log.warn("EMAIL_SKIP ref={} reason=no_recipient_email", ref);
            return false;
        }
        return true;
    }

    /**
     * Builds and sends a MIME HTML email.
     *
     * MessagingException is caught here and re-thrown as MailSendException
     * (a MailException subclass) so @Retryable on the callers can intercept it.
     * Previously it was wrapped in RuntimeException, which @Retryable could not
     * see and therefore never retried MIME-build failures.
     */
    private void sendHtml(String to, String subject, String html) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            try {
                h.setFrom(fromAddress, "OrderFlow");
            } catch (java.io.UnsupportedEncodingException enc) {
                h.setFrom(fromAddress);   // plain address always works
            }
            h.setTo(to);
            h.setSubject(subject);
            h.setText(html, true);
            mailSender.send(msg);         // throws MailException on SMTP failure
        } catch (MessagingException e) {
            // Re-wrap as MailSendException so @Retryable retryFor = MailException catches it
            throw new MailSendException("Failed to build MIME message: " + e.getMessage(), e);
        }
    }

    private String fmt(BigDecimal amount) {
        return amount == null ? "0.00" : String.format("%.2f", amount);
    }
}
