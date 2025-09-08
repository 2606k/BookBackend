package org.fix.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 维修家电后台管理用户信息表
 * @TableName wedding_user
 */
@TableName(value ="wedding_user")
@Data
public class WeddingUser {
    /**
     * 用户唯一ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户昵称
     */
    @TableField(value = "username")
    private String username;

    /**
     * 微信OpenID
     */
    @TableField(value = "openid")
    private String openid;

    /**
     * 头像URL
     */
    @TableField(value = "avatar_url")
    private String avatarUrl;

    /**
     * 手机号
     */
    @TableField(value = "phone")
    private String phone;

    /**
     * 密码
     */
    @TableField(value = "password")
    private String password;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private Date createdat;
}