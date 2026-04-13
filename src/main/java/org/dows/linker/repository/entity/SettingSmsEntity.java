package org.dows.linker.repository.entity;

import com.mybatisflex.annotation.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dows.rade.crud.CrudEntity;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("setting_sms")
public class SettingSmsEntity extends CrudEntity<SettingSmsEntity> {

    @Schema(title = "短信配置ID")
    private Long settingSmsId;
    @Schema(title = "引用ID(引用表ID)")
    private Long referenceId;
    @Schema(title = "引用源[数据表]")
    private String referenceSource;
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