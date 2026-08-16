package com.orderflow.notification.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Configures JavaMailSenderImpl programmatically so SMTP timeouts are
 * guaranteed to apply regardless of how Spring Boot resolves config properties.
 *
 * Without explicit timeouts, a stalled SMTP connection hangs for the OS
 * default (~2 minutes), which blocks the Kafka consumer thread and causes
 * @Retryable to exhaust all attempts during a single network stall.
 *
 * With 10s timeouts each attempt fails fast, @Retryable gets a real second
 * chance, and the consumer thread is not blocked for more than ~35s total
 * (10s × 3 attempts + 5s + 10s backoff).
 */
@Configuration
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);

        Properties props = sender.getJavaMailProperties();

        // Authentication and TLS
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth",           "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");

        // Hard timeouts — these are the raw Jakarta Mail property names.
        // 10 000 ms = 10 seconds per operation.
        // connectiontimeout: time allowed to open the TCP socket
        // timeout:           time waiting for a server response after sending a command
        // writetimeout:      time allowed to write data to the socket
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout",           "10000");
        props.put("mail.smtp.writetimeout",      "10000");

        log.info("MAIL_CONFIG host={} port={} username={} timeouts=10s",
                host, port, username.isBlank() ? "<not-set>" : username);

        return sender;
    }
}
