package org.fix.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@TableName(value ="address")
@Data
public class Address {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * openid
     */
    @TableField(value = "openid")
    private String openid;

    /**
     * 收货人姓名
     */
    @TableField(value = "name")
    private String name;

    /**
     * 收货人手机号
     */
    @TableField(value = "phone")
    private String phone;

    /**
     * 收货地址
     */
    @TableField(value = "address")
    private String address;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private Date createdat;
}
