package com.theplutushome.veristore.service;

import javax.enterprise.context.ApplicationScoped;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class DeliveryService implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(DeliveryService.class.getName());

    private final Deque<OutboxEntry> outbox = new ArrayDeque<>();

    public synchronized void sendEmail(String to, List<String> maskedCodes, String subject, String body) {
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(maskedCodes, "maskedCodes");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(body, "body");
        log("email", to, maskedCodes, subject, body);
    }

    public synchronized void sendSms(String msisdn, List<String> maskedCodes, String body) {
        Objects.requireNonNull(msisdn, "msisdn");
        Objects.requireNonNull(maskedCodes, "maskedCodes");
        Objects.requireNonNull(body, "body");
        log("sms", msisdn, maskedCodes, null, body);
    }

    public synchronized List<OutboxEntry> recentEntries() {
        return Collections.unmodifiableList(new ArrayList<>(outbox));
    }

    private void log(String channel, String recipient, List<String> maskedCodes, String subject, String body) {
        OutboxEntry entry = new OutboxEntry(Instant.now(), channel, recipient, List.copyOf(maskedCodes), subject, body);
        outbox.addFirst(entry);
        LOGGER.log(Level.INFO, () -> String.format("Delivered via %s to %s: %s", channel, recipient, maskedCodes));
        while (outbox.size() > 100) {
            outbox.removeLast();
        }
    }

    public static final class OutboxEntry implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final Instant timestamp;
        private final String channel;
        private final String recipient;
        private final List<String> maskedCodes;
        private final String subject;
        private final String body;

        OutboxEntry(Instant timestamp, String channel, String recipient, List<String> maskedCodes, String subject, String body) {
            this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
            this.channel = Objects.requireNonNull(channel, "channel");
            this.recipient = Objects.requireNonNull(recipient, "recipient");
            this.maskedCodes = List.copyOf(Objects.requireNonNull(maskedCodes, "maskedCodes"));
            this.subject = subject;
            this.body = Objects.requireNonNull(body, "body");
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public String getChannel() {
            return channel;
        }

        public String getRecipient() {
            return recipient;
        }

        public List<String> getMaskedCodes() {
            return maskedCodes;
        }

        public String getSubject() {
            return subject;
        }

        public String getBody() {
            return body;
        }
    }
}
