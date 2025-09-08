package org.fix.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@TableName(value ="Order")
@Data
public class Order {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 购买人微信openid
     */
    @TableField(value = "openid")
    private String openid;

    /**
     * 购买人
     */
    @TableField(value = "name")
    private String name;

    /**
     * 联系电话
     */
    @TableField(value = "phone")
    private String phone;

//    /**
//     * 预约时间
//     */
//    @TableField(value = "time")
//    private Date time;

    /**
     * 购买地址
     */
    @TableField(value = "address")
    private String address;

    /**
     * 购买数量
     */
    @TableField(value = "num")
    private Integer num;

    /**
     * 购买金额
     */
    @TableField(value = "money")
    private Double money;



    /**
     * 购买状态
     */
    @TableField(value = "status")
    private String status;

    /**
     * 购买书籍
     */
    @TableField(value = "book_name")
    private String bookname;

    /**
     * 书籍类型
     */
    @TableField(value = "book_type")
    private String booktype;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private Date createdat;
}
