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
import java.util.List;
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
    private final String PASSWORD = "M.";
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
}
