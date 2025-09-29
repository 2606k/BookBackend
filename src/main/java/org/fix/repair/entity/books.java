package org.fix.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@TableName(value ="books")
@Data
public class books {
    /**
     * 图书ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 分类D
     */
    @TableField(value = "category_id")
    private Long categoryId;

    /**
     * 图书名称
     */
    @TableField(value = "book_name")
    private String bookName;

    /**
     * 作者
     */
    @TableField(value = "author")
    private String author;

    /**
     * 出版社
     */
    @TableField(value = "publisher")
    private String publisher;

    /**
     * 出版日期
     */
    @TableField(value = "publish_date")
    private String publishDate;

    /**
     * 图书图片
     */
    @TableField(value = "image_url")
    private String imageurl;

    /**
     * 图书价格
     */
    @TableField(value = "price")
    private Integer price;

    /**
     * 图书描述
     */
    @TableField(value = "description")
    private String description;

    /**
     * 图书库存
     */
    @TableField(value = "stock")
    private Integer stock;

    /**
     *创建时间
     */
    @TableField(value = "created_at")
    private Date createdat;

}
