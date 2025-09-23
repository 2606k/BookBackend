package org.fix.repair.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.fix.repair.entity.Cart;
import org.fix.repair.common.R;

import java.util.List;
import java.util.Map;

/**
 * 购物车服务接口
 */
public interface CartService extends IService<Cart> {
    
    /**
     * 添加商品到购物车
     * @param cartInfo 购物车信息
     * @return 添加结果
     */
    R<String> addToCart(Map<String, Object> cartInfo);
    
    /**
     * 更新购物车商品数量
     * @param cartId 购物车ID
     * @param quantity 新数量
     * @return 更新结果
     */
    R<String> updateQuantity(Long cartId, Integer quantity);
    
    /**
     * 删除购物车商品
     * @param cartId 购物车ID
     * @return 删除结果
     */
    R<String> removeFromCart(Long cartId);
    
    /**
     * 清空用户购物车
     * @param openid 用户openid
     * @return 清空结果
     */
    R<String> clearCart(String openid);
    
    /**
     * 获取用户购物车列表
     * @param openid 用户openid
     * @return 购物车列表
     */
    R<List<Cart>> getCartList(String openid);
    
    /**
     * 批量选中/取消选中商品
     * @param cartIds 购物车ID列表
     * @param selected 是否选中
     * @return 操作结果
     */
    R<String> batchSelectItems(List<Long> cartIds, Boolean selected);
    
    /**
     * 获取选中商品数量
     * @param openid 用户openid
     * @return 选中商品数量
     */
    R<Integer> getSelectedCount(String openid);
}
