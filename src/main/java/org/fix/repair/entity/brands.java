package org.fix.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@TableName(value ="brands")
@Data
public class brands {
    /**
     * 用户唯一ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 品牌ID
     */
    @TableField(value = "category_id")
    private Long categoryId;

    /**
     * 品牌名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 品牌图片
     */
    @TableField(value = "image_url")
    private String imageurl;

    /**
     *创建时间
     */
    @TableField(value = "created_at")
    private Date createdat;

}
