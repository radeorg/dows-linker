package org.dows.linker.mail;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttachmentSchema {

    @Schema(description = "岗位编号[P_6位数字]")
    private String positionNo;

    @Schema(description = "岗位名称(【技术合伙人_上海_100-120元_时】于先生_8年.pdf)")
    private String positionName;

    @Schema(description = "简历来源[boss|lagou|self...]")
    private String source;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "批次号")
    private Long batchNo;

    @Schema(description = "文件md5")
    private String md5;

    @Schema(description = "文件大小")
    private Long fileSize;

    @Schema(description = "文件类型(扩展名)")
    private String fileExt;

    @Schema(description = "收件时间")
    private LocalDateTime receiveTime;
}


