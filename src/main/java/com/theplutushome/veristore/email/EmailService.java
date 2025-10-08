/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.theplutushome.veristore.email;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.Message;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Locale;

/**
 *
 * @author MalickMoro-Samah
 */
@ApplicationScoped
public class EmailService implements Serializable {

    @Inject
    private EmailTemplateService templateService;
    private Session session;

    private final String USERNAME = "";
    private final String PASSWORD = "";
    private final String SMTP_HOST = "smtp.office365.com";
    private final String SMTP_PORT = "587";
    private final String TIMEOUT = "30000";

    @PostConstruct
    public void init() {
        String username = USERNAME;
        String password = PASSWORD;

        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", SMTP_HOST);
        properties.put("mail.smtp.port", SMTP_PORT);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.starttls.required", "true");
        properties.put("mail.smtp.ssl.protocols", "TLSv1.2");
        properties.put("mail.smtp.ssl.trust", SMTP_HOST);
        properties.put("mail.smtp.connectiontimeout", TIMEOUT);
        properties.put("mail.smtp.timeout", TIMEOUT);
        properties.put("mail.smtp.writetimeout", TIMEOUT);

        session = Session.getInstance(properties, new jakarta.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    public boolean testConnection() {
        try {
            try (Transport transport = session.getTransport("smtp")) {
                transport.connect();
            }
            System.out.println("Successfully connected to Office 365 SMTP server");
            return true;
        } catch (MessagingException e) {
            System.err.println("Failed to connect to Office 365 SMTP server: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private Locale getLocale() {
        // In the future, this could be dynamic per user
        return Locale.ENGLISH;
    }

    private boolean sendEmailAsHtml(String subject, String recipient, String htmlContent) {
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME, false));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient, false));
            message.setSubject(subject);
            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("HTML email sent successfully to: " + recipient);
            return true;

        } catch (MessagingException e) {
            System.err.println("Failed to send HTML email: " + e.getMessage());
            return false;
        }
    }

    private boolean sendEmailAsText(String subject, String recipient, String textContent) {
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME, false));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient, false));
            message.setSubject(subject);
            message.setText(textContent, "utf-8", "plain");

            Transport.send(message);
            System.out.println("Text email sent successfully to: " + recipient);
            return true;

        } catch (MessagingException e) {
            System.err.println("Failed to send text email: " + e.getMessage());
            return false;
        }
    }

    private boolean sendEmailWithAttachment(String subject, String recipient, String body, List<File> attachments) {
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            message.setSubject(subject);

            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(body);

            MimeBodyPart attachmentPart = new MimeBodyPart();
            for (File attachment : attachments) {
                attachmentPart.attachFile(attachment);
            }

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            multipart.addBodyPart(attachmentPart);

            message.setContent(multipart);
            Transport.send(message);
            System.out.println("Email sent successfully.");
            return true;
        } catch (MessagingException | IOException mex) {
            System.out.println("Email could not be sent." + mex.getMessage());
            mex.printStackTrace();
        }
        return false;
    }

    public boolean sendPinsEmail(String recipient, String reference, Map<String, List<String>> pinsByProduct) {
        if (recipient == null || recipient.isBlank() || pinsByProduct == null || pinsByProduct.isEmpty()) {
            return false;
        }
        Map<String, List<String>> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : pinsByProduct.entrySet()) {
            List<String> codes = entry.getValue();
            if (codes == null || codes.isEmpty()) {
                continue;
            }
            sanitized.put(entry.getKey(), List.copyOf(codes));
        }
        if (sanitized.isEmpty()) {
            return false;
        }
        String subject = String.format("Your Veristore PINs for %s", reference);
        String htmlBody = buildPinsHtmlBody(reference, sanitized);
        if (sendEmailAsHtml(subject, recipient, htmlBody)) {
            return true;
        }
        String textBody = buildPinsTextBody(reference, sanitized);
        return sendEmailAsText(subject, recipient, textBody);
    }

    private String buildPinsHtmlBody(String reference, Map<String, List<String>> pinsByProduct) {
        StringBuilder builder = new StringBuilder();
        builder.append("<p>Hello,</p>");
        builder.append("<p>Thank you for your purchase. Here are your PINs for reference <strong>")
                .append(reference)
                .append("</strong>:</p>");

        for (Map.Entry<String, List<String>> entry : pinsByProduct.entrySet()) {
            String description = entry.getKey();
            List<String> pins = entry.getValue();
            builder.append("<p><strong>Product:</strong> ")
                    .append(description == null || description.isBlank() ? "Veristore product" : description)
                    .append("</p>");
            builder.append("<ul>");
            for (String pin : pins) {
                builder.append("<li><strong>")
                        .append(pin)
                        .append("</strong></li>");
            }
            builder.append("</ul>");
        }
        builder.append("<p>Please keep these codes safe.\nIf you did not request this delivery, contact our support team immediately.</p>");
        builder.append("<p>Regards,<br/>Veristore Team</p>");
        return builder.toString();
    }

    private String buildPinsTextBody(String reference, Map<String, List<String>> pinsByProduct) {
        StringBuilder builder = new StringBuilder();
        builder.append("Hello,\n\n");
        builder.append("Thank you for your purchase. Here are your PINs for reference ")
                .append(reference)
                .append(":\n");

        for (Map.Entry<String, List<String>> entry : pinsByProduct.entrySet()) {
            String description = entry.getKey();
            List<String> pins = entry.getValue();
            builder.append("\nProduct: ")
                    .append(description == null || description.isBlank() ? "Veristore product" : description)
                    .append("\n");
            for (String pin : pins) {
                builder.append(" - ")
                        .append(pin)
                        .append('\n');
            }
            builder.append('\n');
        }
        builder.append("Please keep these codes safe. If you did not request this delivery, contact our support team immediately.\n\n");
        builder.append("Regards,\nVeristore Team");
        return builder.toString();
    }
}
