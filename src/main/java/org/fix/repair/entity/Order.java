package org.fix.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;
import java.util.List;

@TableName(value ="orders")
@Data
public class Order {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订单号
     */
    @TableField(value = "out_trade_no")
    private String outTradeNo;

    /**
     * 交易号
     */
    @TableField(value = "transaction_id")
    private String transactionId;

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

    /**
     * 订单备注
     */
    @TableField(value = "remark")
    private String remark;

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
    private Integer money;


    /**
     * 购买状态  0:已支付 1：申请退款 2:已退款
     */
    @TableField(value = "status")
    private String status;

    /**
     * 订单取货类型
     */
    @TableField(value = "delivery_type")
    private String deliveryType;


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
     * 支付时间
     */
    @TableField(value = "pay_time")
    private Date payTime;

    /**
     * 退款时间
     */
    @TableField(value = "refund_time")
    private Date refundTime;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private Date createdat;

    /**
     * 订单项列表（不映射到数据库）
     */
    @TableField(exist = false)
    private List<OrderItem> orderItems;
}
