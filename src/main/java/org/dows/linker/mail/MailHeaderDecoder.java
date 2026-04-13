package org.dows.linker.mail;

import jakarta.mail.Address;
import jakarta.mail.internet.MimeUtility;

import java.io.UnsupportedEncodingException;

/**
 * 邮件头信息解码工具类
 */
public class MailHeaderDecoder {

    /**
     * 解码邮件发件人信息
     */
    public static String decodeFromAddress(Address[] fromAddresses) throws UnsupportedEncodingException {
        if (fromAddresses == null || fromAddresses.length == 0) {
            return "";
        }
        return MimeUtility.decodeText(fromAddresses[0].toString());
    }

    /**
     * 解码邮件收件人信息
     */
    public static String decodeToAddress(Address[] toAddresses) throws UnsupportedEncodingException {
        if (toAddresses == null || toAddresses.length == 0) {
            return "";
        }
        return MimeUtility.decodeText(toAddresses[0].toString());
    }

    /**
     * 解码邮件主题
     */
    public static String decodeSubject(String subject) throws UnsupportedEncodingException {
        if (subject == null) {
            return "";
        }
        return MimeUtility.decodeText(subject);
    }

    /**
     * 解码邮件地址数组
     */
    public static String[] decodeAddresses(Address[] addresses) throws UnsupportedEncodingException {
        if (addresses == null) {
            return new String[0];
        }
        String[] decodedAddresses = new String[addresses.length];
        for (int i = 0; i < addresses.length; i++) {
            decodedAddresses[i] = MimeUtility.decodeText(addresses[i].toString());
        }
        return decodedAddresses;
    }
}
