package org.fix.repair.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.fix.repair.entity.Cart;

import java.util.List;

/**
 * 购物车Service接口
 */
public interface CartService extends IService<Cart> {
    
    /**
     * 根据用户openid获取购物车列表
     */
    List<Cart> getCartByOpenid(String openid);
    
    /**
     * 添加商品到购物车
     */
    boolean addToCart(String openid, Long bookId, Integer quantity);
    
    /**
     * 更新购物车商品数量
     */
    boolean updateCartQuantity(Long cartId, Integer quantity);
    
    /**
     * 更新购物车商品选中状态
     */
    boolean updateCartSelected(Long cartId, Boolean selected);
    
    /**
     * 批量更新购物车商品选中状态
     */
    boolean updateCartSelectedBatch(String openid, List<Long> cartIds, Boolean selected);
    
    /**
     * 删除购物车商品
     */
    boolean removeFromCart(Long cartId);
    
    /**
     * 批量删除购物车商品
     */
    boolean removeFromCartBatch(String openid, List<Long> cartIds);
    
    /**
     * 清空用户购物车
     */
    boolean clearCart(String openid);
    
    /**
     * 获取用户购物车商品数量
     */
    Integer getCartCount(String openid);
    
    /**
     * 获取用户选中商品的总金额
     */
    Integer getSelectedTotalAmount(String openid);
} 