package org.dows.linker.repository.entity;

import com.mybatisflex.annotation.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dows.rade.crud.CrudEntity;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("setting_mail")
public class SettingMailEntity extends CrudEntity<SettingMailEntity> {

    @Schema(title = "简历邮箱配置ID")
    private Long emailSettingId;
    @Schema(title = "引用ID(引用表ID)")
    private Long referenceId;
    @Schema(title = "引用源[数据表]")
    private String referenceSource;
    @Schema(title = "邮箱通道（163")
    private String channel;
    @Schema(title = "邮箱地址(123@qq.com）")
    private String emailAddress;
    @Schema(title = "加密授权码")
    private String authCode;
    @Schema(title = "协议（IMAP")
    private String protocol;
    @Schema(title = "服务器地址")
    private String mailHost;
    @Schema(title = "服务器端口")
    private Integer mailPort;
    @Schema(title = "邮箱类型（0:个人，1:企业）")
    private Integer emailType;
    @Schema(title = "轮询间隔（毫秒）")
    private Long pollInterval;
    @Schema(title = "邮箱文件夹")
    private String folder;
    @Schema(title = "是否启用SSL")
    private Boolean sslEnabled;
    @Schema(title = "是否标记为已读")
    private Boolean  shouldMarkAsRead;
    @Schema(title = "是否删除邮件")
    private Boolean shouldDeleteMessages;
    @Schema(title = "最大获取数量")
    private Integer maxFetchSize;
    @Schema(title = "状态（NORMAL-正常")
    private Integer state;
    @Schema(title = "版本号")
    private Integer revision;
    @Schema(title = "应用id")
    private String appId;
    @Schema(title = "时间戳")
    private LocalDateTime createTime;
    @Schema(title = "更新时间")
    private LocalDateTime updateTime;
    @Schema(title = "删除时间")
    private LocalDateTime deleteTime;
    @Schema(title = "创建者ID")
    private Long createId;
    @Schema(title = "更新者ID")
    private Long updateId;
}