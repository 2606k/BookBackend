package org.fix.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@TableName(value ="appointments")
@Data
public class appointments {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 预约人微信openid
     */
    @TableField(value = "openid")
    private String openid;

    /**
     * 预约人
     */
    @TableField(value = "name")
    private String name;

    /**
     * 联系电话
     */
    @TableField(value = "phone")
    private String phone;

    /**
     * 预约时间
     */
    @TableField(value = "time")
    private Date time;

    /**
     * 预约地址
     */
    @TableField(value = "address")
    private String address;

    /**
     * 预约状态
     */
    @TableField(value = "status")
    private String status;

    /**
     * 修理品牌
     */
    @TableField(value = "brand_name")
    private String brandname;

    /**
     * 修理类型
     */
    @TableField(value = "repair_type")
    private String repairtype;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private Date createdat;
}
