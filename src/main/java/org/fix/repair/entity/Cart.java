package org.fix.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 购物车实体类
 */
@TableName(value = "cart")
@Data
public class Cart {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户微信openid
     */
    @TableField(value = "openid")
    private String openid;

    /**
     * 书籍ID
     */
    @TableField(value = "book_id")
    private Long bookId;

    /**
     * 书籍名称
     */
    @TableField(value = "book_name")
    private String bookName;

    /**
     * 书籍价格（单价，单位：分）
     */
    @TableField(value = "price")
    private Integer price;

    /**
     * 购买数量
     */
    @TableField(value = "quantity")
    private Integer quantity;

    /**
     * 是否选中（用于结算）
     */
    @TableField(value = "selected")
    private Boolean selected;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private Date createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at")
    private Date updatedAt;
} 