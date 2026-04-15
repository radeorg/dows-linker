package org.dows.linker.mail;

import cn.hutool.core.bean.BeanUtil;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dows.linker.repository.dao.SettingMailDao;
import org.dows.linker.repository.entity.SettingMailEntity;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.mail.ImapMailReceiver;
import org.springframework.integration.mail.MailReceiver;
import org.springframework.integration.mail.MailReceivingMessageSource;
import org.springframework.messaging.Message;
import org.springframework.scheduling.TaskScheduler;

import java.util.*;

@RequiredArgsConstructor
@Slf4j
@Configuration
@EnableIntegration
public class MailReceiverConfiguration implements ApplicationRunner {


    private final MailReceivable mailReceivable;
    private final TaskScheduler taskScheduler;

    private final SettingMailDao settingMailDao;

    // 存储所有邮件接收相关组件
    private final Map<String, MailReceivingMessageSource> messageSources = new HashMap<>();
    private final Map<String, DirectChannel> channels = new HashMap<>();
    private final Map<String, MailReceiver> receivers = new HashMap<>();


    /**
     * 处理接收到的邮件消息
     *
     * @param message 邮件消息
     */
    @ServiceActivator(inputChannel = "mailHandlerChannel")
    public void handleReceivedMail(Message<?> message) {
        mailReceivable.receive((MimeMessage) message.getPayload());
    }

    /**
     * 创建通用的邮件处理通道
     *
     * @return DirectChannel
     */
    @Bean("mailHandlerChannel")
    public DirectChannel mailHandlerChannel() {
        DirectChannel directChannel = new DirectChannel();
        directChannel.setDatatypes(MimeMessage.class);
        return directChannel;
    }

    /**
     * 应用启动后初始化所有邮件服务器配置
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("开始初始化邮件服务器配置...");
        // 从数据库加载所有邮件服务器配置
        List<SettingMailEntity> settingMailEntities = settingMailDao.list();
        //List<SettingMailEntity> settingMailEntities = new ArrayList<>();
        log.info("加载到 {} 个邮件服务器配置", settingMailEntities.size());

        for (SettingMailEntity setting : settingMailEntities) {
            try {
                MailSetting config = BeanUtil.copyProperties(setting, MailSetting.class);
                // 为每个配置创建独立的通道
                DirectChannel channel = createChannel(config);
                channels.put(config.getKey(), channel);

                // 为每个配置创建邮件接收器
                MailReceiver receiver = createMailReceiver(config);
                receivers.put(config.getKey(), receiver);

                // 为每个配置创建邮件接收消息源
                MailReceivingMessageSource messageSource = createMailMessageSource(receiver, channel);
                messageSources.put(config.getKey(), messageSource);

                // 启动定时任务
                startScheduledTask(config, messageSource);

                log.info("已为用户 {} 创建邮件接收组件，轮询间隔：{}ms", config.getKey(), config.getPollInterval());
            } catch (Exception e) {
                log.error("为配置 {} 创建邮件接收组件失败：{}", setting, e.getMessage(), e);
            }
        }
    }

    /**
     * 创建邮件接收通道
     *
     * @param config 邮件服务器配置
     * @return DirectChannel
     */
    private DirectChannel createChannel(MailSetting config) {
        DirectChannel channel = new DirectChannel();
        channel.setDatatypes(MimeMessage.class);

        // 注册通道的消息处理器
        channel.subscribe(message -> {
            log.debug("从配置 {} 接收到邮件，转发到通用处理通道", config);
            mailReceivable.receive((MimeMessage) message.getPayload());
        });

        return channel;
    }

    /**
     * 创建邮件接收器
     *
     * @param config 邮件服务器配置
     * @return MailReceiver
     */
    private MailReceiver createMailReceiver(MailSetting config) {
        String storeUrl = buildStoreUrl(config);
        ImapMailReceiver receiver = new ImapMailReceiver(storeUrl);

        // 配置接收器属性
        receiver.setShouldMarkMessagesAsRead(config.getShouldMarkAsRead());
        receiver.setShouldDeleteMessages(config.getShouldDeleteMessages());
        receiver.setMaxFetchSize(config.getMaxFetchSize());
        receiver.setJavaMailProperties(buildJavaMailProperties(config));

        return receiver;
    }

    /**
     * 创建邮件接收消息源
     *
     * @param receiver 邮件接收器
     * @param channel  邮件接收通道
     * @return MailReceivingMessageSource
     */
    private MailReceivingMessageSource createMailMessageSource(MailReceiver receiver, DirectChannel channel) {
        return new MailReceivingMessageSource(receiver);
    }

    /**
     * 启动定时任务
     *
     * @param config        邮件服务器配置
     * @param messageSource 邮件接收消息源
     */
    private void startScheduledTask(MailSetting config, MailReceivingMessageSource messageSource) {
        // 使用Spring的TaskScheduler启动定时任务
        taskScheduler.scheduleAtFixedRate(() -> {
            try {
                //log.info("执行邮件轮询任务，配置ID：{}", config.getKey());
                // 获取新邮件并发送到通道
                Message<?> message = messageSource.receive();
                if (message != null) {
                    log.info("接收到新邮件，配置ID：{}", config.getKey());
                    channels.get(config.getKey()).send(message);
                } /*else {
                    log.debug("未接收到新邮件，配置ID：{}", config.getKey());
                }*/
            } catch (Exception e) {
                log.error("邮件轮询任务执行失败，配置ID：{}", config.getKey(), e);
            }
        }, java.time.Duration.ofMillis(config.getPollInterval()));
    }

    /**
     * 构建邮件服务器连接URL
     *
     * @param config 邮件服务器配置
     * @return 连接URL
     */
    /*private String buildStoreUrl(MailProperties config) {
        // 处理特殊字符，如@需要编码为%40
        String encodedUsername = config.getEmailAddress().replace("@", "%40");
        String encodedPassword = config.getAuthCode().replace("@", "%40");

        String protocol = config.getSslEnabled() ? config.getProtocol() + "s" : config.getProtocol();
        return String.format("%s://%s:%s@%s:%d/%s",
                protocol, encodedUsername, encodedPassword,
                config.getMailHost(), config.getMailPort(), config.getFolder());
    }*/
    private String buildStoreUrl(MailSetting config) {
        try {
            String encodedUsername = java.net.URLEncoder.encode(config.getEmailAddress(), java.nio.charset.StandardCharsets.UTF_8);
            String encodedPassword = java.net.URLEncoder.encode(config.getAuthCode(), java.nio.charset.StandardCharsets.UTF_8);

            String protocol = config.getSslEnabled() ? config.getProtocol() + "s" : config.getProtocol();
            return String.format("%s://%s:%s@%s:%d/%s",
                    protocol, encodedUsername, encodedPassword,
                    config.getMailHost(), config.getMailPort(), config.getFolder());
        } catch (Exception e) {
            log.error("构建邮件服务器URL失败: {}", config.getEmailAddress(), e);
            throw new RuntimeException("构建邮件服务器URL失败", e);
        }
    }

    /**
     * 构建JavaMail属性
     *
     * @param config 邮件服务器配置
     * @return JavaMail属性
     */
    private Properties buildJavaMailProperties(MailSetting config) {
        Properties properties = new Properties();

        String protocol = config.getProtocol();
        String sslProtocol = config.getSslEnabled() ? protocol + "s" : protocol;

        properties.put("mail.store.protocol", sslProtocol);
        properties.put("mail.debug", "false");

        // SSL配置
        if (config.getSslEnabled()) {
            properties.put(String.format("mail.%s.socketFactory.class", protocol), "jakarta.mail.ssl.SSLSocketFactory");
            properties.put(String.format("mail.%s.socketFactory.fallback", protocol), "false");
            properties.put(String.format("mail.%s.socketFactory.port", protocol), config.getMailPort().toString());

            properties.put(String.format("mail.%s.ssl.enable", protocol), "true");
        } else {
//            properties.put(String.format("mail.%s.starttls.enable", protocol), "true");
//            properties.put(String.format("mail.%s.starttls.required", protocol), "true");
        }

//        properties.put(String.format("mail.%s.connectiontimeout", protocol), "30000");
//        properties.put(String.format("mail.%s.timeout", protocol), "30000");
//        properties.put(String.format("mail.%s.writetimeout", protocol), "30000");

        return properties;
    }
}
