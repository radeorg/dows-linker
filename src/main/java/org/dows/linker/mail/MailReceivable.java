package org.dows.linker.mail;

import jakarta.mail.internet.MimeMessage;

public interface MailReceivable {
    void receive(MimeMessage payload);
}
