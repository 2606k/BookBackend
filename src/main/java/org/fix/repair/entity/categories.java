package org.fix.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@TableName(value ="categories")
@Data
public class categories {
    @TableId(value = "service_type_id", type = IdType.AUTO)
    private Long servicetypeid;

    /**
     * 分类名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 分类图标
     */
    @TableField(value = "image_url")
    private String imageurl;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "created_at")
    private Date createdat;
}
