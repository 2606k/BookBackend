package org.fix.repair.controller;

import lombok.extern.slf4j.Slf4j;
import org.fix.repair.common.R;
import org.fix.repair.entity.Cart;
import org.fix.repair.service.CartService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 购物车控制器
 * @author tangxin
 */
@Slf4j
@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * 添加商品到购物车
     * @param cartInfo 购物车信息：{ openid, bookId, quantity }
     * @return 添加结果
     */
    @PostMapping("/add")
    public R<String> addToCart(@RequestBody Map<String, Object> cartInfo) {
        try {
            log.info("添加商品到购物车，参数: {}", cartInfo);
            return cartService.addToCart(cartInfo);
        } catch (Exception e) {
            log.error("添加商品到购物车失败", e);
            return R.error("添加失败：" + e.getMessage());
        }
    }

    /**
     * 更新购物车商品数量
     * @param cartId 购物车ID
     * @param quantity 新数量
     * @return 更新结果
     */
    @PutMapping("/update/{cartId}")
    public R<String> updateQuantity(@PathVariable Long cartId, @RequestParam Integer quantity) {
        try {
            log.info("更新购物车数量，购物车ID: {}, 数量: {}", cartId, quantity);
            return cartService.updateQuantity(cartId, quantity);
        } catch (Exception e) {
            log.error("更新购物车数量失败", e);
            return R.error("更新失败：" + e.getMessage());
        }
    }

    /**
     * 删除购物车商品
     * @param cartId 购物车ID
     * @return 删除结果
     */
    @DeleteMapping("/remove/{cartId}")
    public R<String> removeFromCart(@PathVariable Long cartId) {
        try {
            log.info("删除购物车商品，购物车ID: {}", cartId);
            return cartService.removeFromCart(cartId);
        } catch (Exception e) {
            log.error("删除购物车商品失败", e);
            return R.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 清空用户购物车
     * @param openid 用户openid
     * @return 清空结果
     */
    @DeleteMapping("/clear")
    public R<String> clearCart(@RequestParam String openid) {
        try {
            log.info("清空购物车，用户openid: {}", openid);
            return cartService.clearCart(openid);
        } catch (Exception e) {
            log.error("清空购物车失败", e);
            return R.error("清空失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户购物车列表
     * @param openid 用户openid
     * @return 购物车列表
     */
    @GetMapping("/list")
    public R<List<Cart>> getCartList(@RequestParam String openid) {
        try {
            log.info("获取购物车列表，用户openid: {}", openid);
            return cartService.getCartList(openid);
        } catch (Exception e) {
            log.error("获取购物车列表失败", e);
            return R.error("获取失败：" + e.getMessage());
        }
    }

    /**
     * 批量选中/取消选中商品
     * @param params 参数：{ cartIds: [1,2,3], selected: true/false }
     * @return 操作结果
     */
    @PutMapping("/select")
    public R<String> batchSelectItems(@RequestBody Map<String, Object> params) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> cartIds = (List<Long>) params.get("cartIds");
            Boolean selected = (Boolean) params.get("selected");
            
            log.info("批量选中商品，购物车ID列表: {}, 选中状态: {}", cartIds, selected);
            return cartService.batchSelectItems(cartIds, selected);
        } catch (Exception e) {
            log.error("批量选中商品失败", e);
            return R.error("操作失败：" + e.getMessage());
        }
    }

    /**
     * 单个商品选中/取消选中
     * @param cartId 购物车ID
     * @param selected 是否选中
     * @return 操作结果
     */
    @PutMapping("/select/{cartId}")
    public R<String> selectItem(@PathVariable Long cartId, @RequestParam Boolean selected) {
        try {
            log.info("选中/取消选中商品，购物车ID: {}, 选中状态: {}", cartId, selected);
            return cartService.batchSelectItems(List.of(cartId), selected);
        } catch (Exception e) {
            log.error("选中/取消选中商品失败", e);
            return R.error("操作失败：" + e.getMessage());
        }
    }

    /**
     * 获取选中商品数量
     * @param openid 用户openid
     * @return 选中商品数量
     */
    @GetMapping("/selected/count")
    public R<Integer> getSelectedCount(@RequestParam String openid) {
        try {
            log.info("获取选中商品数量，用户openid: {}", openid);
            return cartService.getSelectedCount(openid);
        } catch (Exception e) {
            log.error("获取选中商品数量失败", e);
            return R.error("获取失败：" + e.getMessage());
        }
    }

    /**
     * 获取选中的购物车项（用于结算）
     * @param openid 用户openid
     * @return 选中的购物车项列表
     */
    @GetMapping("/selected")
    public R<List<Cart>> getSelectedItems(@RequestParam String openid) {
        try {
            log.info("获取选中的购物车项，用户openid: {}", openid);
            R<List<Cart>> result = cartService.getCartList(openid);
            if (result.getCode().equals(200) && result.getData() != null) {
                List<Cart> selectedItems = result.getData().stream()
                        .filter(Cart::getSelected)
                        .toList();
                return R.ok(selectedItems);
            }
            return result;
        } catch (Exception e) {
            log.error("获取选中的购物车项失败", e);
            return R.error("获取失败：" + e.getMessage());
        }
    }
}
