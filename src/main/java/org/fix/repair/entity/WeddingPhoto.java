package org.fix.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 婚礼小程序 - 照片瀑布流（内容展示模块）
 * @TableName wedding_photo
 */
@TableName(value ="wedding_photo")
@Data
public class WeddingPhoto {
    /**
     * 照片唯一ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联故事ID（可选，用于故事配图）
     */
    @TableField(value = "story_id")
    private Long storyId;

    /**
     * 照片存储路径（如OSS/服务器地址）
     */
    @TableField(value = "url")
    private String url;

    /**
     * 照片描述（如“第一次旅行合影”）
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 上传时间
     */
    @TableField(value = "create_time")
    private Date createTime;
}