package org.dows.linker.verticle;

import io.vertx.core.Vertx;
import org.dows.rade.event.VertxDomainEventBus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VertxConfiguration {
    /**
     * 创建并配置Vertx实例
     *
     * @return Vertx实例
     */
    @Bean
    public Vertx vertx() {
        // 获取Vert.x默认的ObjectMapper
        com.fasterxml.jackson.databind.ObjectMapper mapper = io.vertx.core.json.jackson.DatabindCodec.mapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        // 注册JavaTimeModule以支持Java 8日期时间类型
        mapper.setDateFormat(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        // 禁用将日期时间序列化为时间戳（默认为数组格式）
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return Vertx.vertx();
    }


    @Bean
    public VertxDomainEventBus vertxDomainEventBus() {
        return new VertxDomainEventBus(vertx());
    }


}
