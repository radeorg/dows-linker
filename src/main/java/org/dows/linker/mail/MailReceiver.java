package org.dows.linker.mail;

import cn.hutool.crypto.digest.MD5;
import jakarta.mail.*;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.dows.rade.event.DomainEvent;
import org.dows.rade.event.DomainEventBus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 邮件接收器
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class MailReceiver implements MailReceivable {


    @Value("${setting.mail.folders.download:DOWNLOAD}")
    private String DOWNLOADED_MAIL_FOLDER;
    @Value("${setting.download.folder:data}")
    private String ATTACHMENT_SAVE_FOLDER;
    @Value("${setting.extractor.keywords:''}")
    private List<String> keywords;
    private final DomainEventBus domainEventBus;
    private final MD5 md5 = MD5.create();

    public void receive(MimeMessage receivedMessage) {
        try {

            Folder folder = receivedMessage.getFolder();
            //folder.open(Folder.READ_WRITE);
            folder.open(Folder.READ_ONLY);
            Message[] messages = folder.getMessages();
            fetchMessagesInFolder(folder, messages);

            Arrays.stream(messages).filter(message -> {
                MimeMessage currentMessage = (MimeMessage) message;
                try {
                    return currentMessage.getMessageID().equalsIgnoreCase(receivedMessage.getMessageID());
                } catch (MessagingException e) {
                    log.error("Error occurred during process message", e);
                    return false;
                }
            }).forEach(this::extractMail);
            // copy mail to downloaded folder
            copyMailToDownloadedFolder(receivedMessage, folder);

            folder.close(true);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }


    /**
     * 解析并下载邮件附件到本地指定目录
     */
    private void downloadMailAttachments(Message message) throws Exception {
        System.out.println("3. 邮件附件处理");
        // 先创建附件保存目录（若不存在）
        File attachmentDir = new File(ATTACHMENT_SAVE_FOLDER);
        if (!attachmentDir.exists()) {
            attachmentDir.mkdirs();
        }

        Object content = message.getContent();
        if (content instanceof MimeMultipart) {
            MimeMultipart multipart = (MimeMultipart) content;
            int partCount = multipart.getCount();
            boolean hasAttachment = false;

            for (int i = 0; i < partCount; i++) {
                MimeBodyPart bodyPart = (MimeBodyPart) multipart.getBodyPart(i);
                // 判断是否为附件（两种判断方式：1. 有Disposition=ATTACHMENT；2. 有文件名且非正文类型）
                boolean isAttachment = Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())
                        || (bodyPart.getFileName() != null && !bodyPart.isMimeType("text/plain") && !bodyPart.isMimeType("text/html"));

                if (isAttachment) {
                    hasAttachment = true;
                    String fileName = bodyPart.getFileName();
                    // 解决中文文件名乱码问题
                    fileName = new String(fileName.getBytes("ISO-8859-1"), "UTF-8");
                    String attachmentPath = ATTACHMENT_SAVE_FOLDER + File.separator + fileName;

                    // 下载附件到本地
                    try (InputStream in = bodyPart.getInputStream();
                         FileOutputStream out = new FileOutputStream(attachmentPath)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = in.read(buffer)) != -1) {
                            out.write(buffer, 0, len);
                        }
                    }
                    System.out.println("   已下载附件：" + fileName + "，保存路径：" + attachmentPath);
                }
            }

            if (!hasAttachment) {
                System.out.println("   该邮件无附件");
            }
        } else {
            System.out.println("   该邮件无附件");
        }
    }


    private void fetchMessagesInFolder(Folder folder, Message[] messages) throws MessagingException {
        FetchProfile contentsProfile = new FetchProfile();
        contentsProfile.add(FetchProfile.Item.ENVELOPE);
        contentsProfile.add(FetchProfile.Item.CONTENT_INFO);
        contentsProfile.add(FetchProfile.Item.FLAGS);
        contentsProfile.add(FetchProfile.Item.SIZE);
        folder.fetch(messages, contentsProfile);
    }

    private void copyMailToDownloadedFolder(MimeMessage mimeMessage, Folder folder) throws MessagingException {
        Store store = folder.getStore();
        Folder downloadedMailFolder = store.getFolder(DOWNLOADED_MAIL_FOLDER);
        if (downloadedMailFolder.exists()) {
            downloadedMailFolder.open(Folder.READ_WRITE);
            downloadedMailFolder.appendMessages(new MimeMessage[]{mimeMessage});
            downloadedMailFolder.close();
        }
    }

    private void extractMail(Message message) {
        try {
            final MimeMessage messageToExtract = (MimeMessage) message;
            //秦柏杨 | 10年以上，应聘 技术合伙人 | 上海100-120元/时【BOSS直聘】
            String subject = MailHeaderDecoder.decodeSubject(message.getSubject());
            // 发件人
            String from = MailHeaderDecoder.decodeFromAddress(message.getFrom());
            printMailBasicInfo(message);
            //parseMailContent(message);
            //showMailContent(messageToExtract);

            String source = null;
            if (from.contains("boss")) {
                source = "boss";
            }
            String positionName = extractBossPositionName(subject);
            String positionNo = positionName.replaceAll("\\|", "");
            List<File> files = downloadAttachmentFiles(messageToExtract);
            LocalDateTime receivedDate = LocalDateTime
                    .ofInstant(messageToExtract.getReceivedDate().toInstant(), java.time.ZoneId.systemDefault());
            long batchNo = System.currentTimeMillis();
            for (File file : files) {
                AttachmentSchema attachmentSchema = new AttachmentSchema();
                attachmentSchema.setFileName(file.getName());
                attachmentSchema.setFileExt(FilenameUtils.getExtension(file.getName()));
                attachmentSchema.setFilePath(file.getAbsolutePath());
                attachmentSchema.setFileSize(file.length());
                attachmentSchema.setMd5(md5.digestHex(file));
                attachmentSchema.setBatchNo(batchNo);
                attachmentSchema.setPositionName(positionName);
                attachmentSchema.setReceiveTime(receivedDate);
                // 需要手动指定岗位编号
                attachmentSchema.setPositionNo(positionNo);
                attachmentSchema.setSource(source);
                // 转换txt
                transform(file);
                DomainEvent domainEvent = DomainEvent.address("setting.mail.readed").data(attachmentSchema);
                domainEventBus.publish(domainEvent);
            }
            // To delete downloaded email
            //messageToExtract.setFlag(Flags.Flag.DELETED, true);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }


    public void transform(File file) {
        // todo 使用python 转换

        /*try {
            PDDocument doc = Loader.loadPDF(file);
            SkewKeywordTxtExtractor stripper = new SkewKeywordTxtExtractor(keywords);
            Path path = Paths.get(ATTACHMENT_SAVE_FOLDER, file.getName() + ".txt");
            OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(path.toFile()), StandardCharsets.UTF_8);
            stripper.writeText(doc, w);
            doc.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/
    }

    /**
     * 从邮件主题中提取职位信息
     *
     * @param subject 邮件主题，如 "秦柏杨 | 10年以上，应聘 技术合伙人 | 上海100-120元/时【BOSS直聘】"
     * @return 职位名称，如 "技术合伙人"
     */
    private String extractBossPositionName(String subject) {
        if (StringUtils.isBlank(subject)) {
            return "";
        }

        // 使用正则表达式匹配 "应聘 [职位名] |" 模式
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("应聘\\s*([^|]+)\\s*\\|");
        java.util.regex.Matcher matcher = pattern.matcher(subject);

        if (matcher.find()) {
            // 返回匹配到的职位名并去除首尾空格
            return matcher.group(1).trim();
        }
        return "";
    }

    /**
     * 解析邮件基础信息（发件人、收件人、主题、发送时间等）
     */
    private static void printMailBasicInfo(Message message) throws Exception {
        System.out.println("1. 邮件基础信息");
        System.out.println("   邮件主题：" + MailHeaderDecoder.decodeSubject(message.getSubject()));
        System.out.println("   发送时间：" + message.getSentDate());
        System.out.println("   邮件是否已读：" + message.isSet(Flags.Flag.SEEN));
        System.out.println("   发件人：" + MailHeaderDecoder.decodeFromAddress(message.getFrom()));
        System.out.println("   收件人：" + MailHeaderDecoder.decodeToAddress(message.getRecipients(Message.RecipientType.TO)));
    }

    /**
     * 解析邮件正文内容（支持纯文本和HTML格式）
     */
    private static void parseMailContent(Message message) throws Exception {
        System.out.println("2. 邮件正文内容");
        // 邮件内容可能是简单文本或多部分内容（正文+附件）
        Object content = message.getContent();
        if (content instanceof MimeMultipart) {
            // 多部分内容（处理正文，忽略附件）
            MimeMultipart multipart = (MimeMultipart) content;
            int partCount = multipart.getCount();
            for (int i = 0; i < partCount; i++) {
                MimeBodyPart bodyPart = (MimeBodyPart) multipart.getBodyPart(i);
                // 判断是否为附件（附件通常有文件名）
                if (bodyPart.isMimeType("text/plain") && !Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                    System.out.println("   纯文本正文：" + bodyPart.getContent());
                } else if (bodyPart.isMimeType("text/html") && !Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                    System.out.println("   HTML格式正文：" + bodyPart.getContent());
                }
            }
        } else {
            // 简单文本内容
            System.out.println("   纯文本正文：" + content);
        }
    }

    private void showMailContent(MimeMessage mimeMessage) throws Exception {
        String from = Arrays.toString(mimeMessage.getFrom());
        String to = Arrays.toString(mimeMessage.getRecipients(Message.RecipientType.TO));
        String subject = mimeMessage.getSubject();
        String content = getPlainContent(mimeMessage);
        log.debug("From: {} to: {} | Subject: {}", from, to, subject);
        log.debug("Mail content: {}", content);
    }

    private String getPlainContent(MimeMessage mimeMessage) throws Exception {
        Object content = mimeMessage.getContent();
        if (content instanceof String) {
            return (String) content;
        } else if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) &&
                        bodyPart.getFileName() == null && "text/plain".equals(bodyPart.getContentType())) {
                    return (String) bodyPart.getContent();
                }
            }
        }
        return "";
    }

    private List<File> downloadAttachmentFiles(MimeMessage mimeMessage) throws Exception {
        Object content = mimeMessage.getContent();
        List<File> downloadedFiles = new ArrayList<>();
        if (content instanceof Multipart multipart) {
            int attachmentCount = 0;
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) || bodyPart.getFileName() != null) {
                    attachmentCount++;
                    String fileName = bodyPart.getFileName();
                    if (StringUtils.isNotBlank(fileName)) {
                        // 使用MimeUtility解码文件名，处理中文文件名
                        fileName = MimeUtility.decodeText(fileName);
                        // 清理文件名中的非法字符
                        fileName = sanitizeFileName(fileName);

                        createDirectoryIfNotExists(ATTACHMENT_SAVE_FOLDER);
                        Path path = Paths.get(ATTACHMENT_SAVE_FOLDER, fileName);
                        String downloadedAttachmentFilePath = path.toString();
                        log.info("Save attachment file to: {}", downloadedAttachmentFilePath);

                        File downloadedAttachmentFile = path.toFile();
                        try (InputStream in = bodyPart.getInputStream();
                             OutputStream out = new FileOutputStream(downloadedAttachmentFile)) {
                            IOUtils.copy(in, out);
                        } catch (IOException e) {
                            log.error("Failed to save file: {}", downloadedAttachmentFilePath, e);
                        }
                        downloadedFiles.add(downloadedAttachmentFile);
                    }
                }
            }
            log.debug("Email has {} attachment files", attachmentCount);
        } else {
            log.debug("Email has 0 attachment files");
        }
        return downloadedFiles;
    }

    private void createDirectoryIfNotExists(String directoryPath) {
        Path path = Paths.get(directoryPath);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                log.error("An error occurred during create folder: {}", directoryPath, e);
            }
        }
    }

    /**
     * 清理文件名中的非法字符
     * Windows系统中不允许的字符: < > : " | ? * \
     * Linux/Unix系统中不允许的字符: /
     */
    private String sanitizeFileName(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return fileName;
        }

        // 替换Windows和Unix系统中不允许的字符
        return fileName
                .replaceAll("[<>:\"|?*\\\\]", "_") // Windows非法字符替换为下划线
                .replaceAll("/", "_")              // Unix/Linux非法字符替换为下划线
                .replaceAll("[\\x00-\\x1f]", "")   // 移除控制字符
                .trim();
    }
}
