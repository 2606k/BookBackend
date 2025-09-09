package org.fix.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName(value ="order_item")
@Data
public class OrderItem {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订单ID
     */
    @TableField(value = "order_id")
    private Long orderId; // 关联订单ID

    /**
     * 书籍ID
     */
    @TableField(value = "book_id")
    private Long bookId; // 书籍ID

    /**
     * 书籍名称
     */
    @TableField(value = "book_name")
    private String bookName;

    /**
     * 购买数量
     */
    @TableField(value = "quantity")
    private Integer quantity;

    /**
     * 单本书价格
     */
    @TableField(value = "price")
    private Integer price; // 单本书价格
}
