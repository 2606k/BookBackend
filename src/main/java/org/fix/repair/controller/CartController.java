package org.fix.repair.controller;

import lombok.extern.slf4j.Slf4j;
import org.fix.repair.common.R;
import org.fix.repair.entity.Cart;
import org.fix.repair.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 购物车控制器
 */
@Slf4j
@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * 获取购物车列表
     */
    @GetMapping("/list")
    public R<List<Cart>> getCartList(@RequestParam String openid) {
        try {
            if (openid == null || openid.trim().isEmpty()) {
                return R.error("openid不能为空");
            }

            List<Cart> cartList = cartService.getCartByOpenid(openid);
            log.info("获取购物车列表成功，用户: {}, 商品数量: {}", openid, cartList.size());
            return R.ok(cartList);
        } catch (Exception e) {
            log.error("获取购物车列表失败", e);
            return R.error("获取购物车列表失败: " + e.getMessage());
        }
    }

    /**
     * 添加商品到购物车
     */
    @PostMapping("/add")
    public R<String> addToCart(@RequestBody Map<String, Object> params) {
        try {
            String openid = (String) params.get("openid");
            Long bookId = Long.valueOf(params.get("bookId").toString());
            Integer quantity = Integer.valueOf(params.get("quantity").toString());

            // 参数校验
            if (openid == null || openid.trim().isEmpty()) {
                return R.error("openid不能为空");
            }
            if (bookId == null || bookId <= 0) {
                return R.error("商品ID不能为空");
            }
            if (quantity == null || quantity <= 0) {
                return R.error("商品数量必须大于0");
            }

            boolean success = cartService.addToCart(openid, bookId, quantity);
            if (success) {
                log.info("添加购物车成功，用户: {}, 商品: {}, 数量: {}", openid, bookId, quantity);
                return R.ok("添加购物车成功");
            } else {
                return R.error("添加购物车失败");
            }
        } catch (Exception e) {
            log.error("添加购物车失败", e);
            return R.error("添加购物车失败: " + e.getMessage());
        }
    }

    /**
     * 更新购物车商品数量
     */
    @PostMapping("/updateQuantity")
    public R<String> updateQuantity(@RequestBody Map<String, Object> params) {
        try {
            Long cartId = Long.valueOf(params.get("cartId").toString());
            Integer quantity = Integer.valueOf(params.get("quantity").toString());

            // 参数校验
            if (cartId == null || cartId <= 0) {
                return R.error("购物车ID不能为空");
            }
            if (quantity == null || quantity <= 0) {
                return R.error("商品数量必须大于0");
            }

            boolean success = cartService.updateCartQuantity(cartId, quantity);
            if (success) {
                log.info("更新购物车数量成功，购物车ID: {}, 新数量: {}", cartId, quantity);
                return R.ok("更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("更新购物车数量失败", e);
            return R.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 更新购物车商品选中状态
     */
    @PostMapping("/updateSelected")
    public R<String> updateSelected(@RequestBody Map<String, Object> params) {
        try {
            Long cartId = Long.valueOf(params.get("cartId").toString());
            Boolean selected = Boolean.valueOf(params.get("selected").toString());

            // 参数校验
            if (cartId == null || cartId <= 0) {
                return R.error("购物车ID不能为空");
            }

            boolean success = cartService.updateCartSelected(cartId, selected);
            if (success) {
                log.info("更新购物车选中状态成功，购物车ID: {}, 选中状态: {}", cartId, selected);
                return R.ok("更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("更新购物车选中状态失败", e);
            return R.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 批量更新购物车商品选中状态
     */
    @PostMapping("/updateSelectedBatch")
    public R<String> updateSelectedBatch(@RequestBody Map<String, Object> params) {
        try {
            String openid = (String) params.get("openid");
            List<Long> cartIds = (List<Long>) params.get("cartIds");
            Boolean selected = Boolean.valueOf(params.get("selected").toString());

            // 参数校验
            if (openid == null || openid.trim().isEmpty()) {
                return R.error("openid不能为空");
            }
            if (cartIds == null || cartIds.isEmpty()) {
                return R.error("购物车ID列表不能为空");
            }

            boolean success = cartService.updateCartSelectedBatch(openid, cartIds, selected);
            if (success) {
                log.info("批量更新购物车选中状态成功，用户: {}, 数量: {}, 选中状态: {}", 
                        openid, cartIds.size(), selected);
                return R.ok("更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("批量更新购物车选中状态失败", e);
            return R.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除购物车商品
     */
    @PostMapping("/remove")
    public R<String> removeFromCart(@RequestParam Long cartId) {
        try {
            if (cartId == null || cartId <= 0) {
                return R.error("购物车ID不能为空");
            }

            boolean success = cartService.removeFromCart(cartId);
            if (success) {
                log.info("删除购物车商品成功，购物车ID: {}", cartId);
                return R.ok("删除成功");
            } else {
                return R.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除购物车商品失败", e);
            return R.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除购物车商品
     */
    @PostMapping("/removeBatch")
    public R<String> removeFromCartBatch(@RequestBody Map<String, Object> params) {
        try {
            String openid = (String) params.get("openid");
            List<Long> cartIds = (List<Long>) params.get("cartIds");

            // 参数校验
            if (openid == null || openid.trim().isEmpty()) {
                return R.error("openid不能为空");
            }
            if (cartIds == null || cartIds.isEmpty()) {
                return R.error("购物车ID列表不能为空");
            }

            boolean success = cartService.removeFromCartBatch(openid, cartIds);
            if (success) {
                log.info("批量删除购物车商品成功，用户: {}, 数量: {}", openid, cartIds.size());
                return R.ok("删除成功");
            } else {
                return R.error("删除失败");
            }
        } catch (Exception e) {
            log.error("批量删除购物车商品失败", e);
            return R.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 清空购物车
     */
    @PostMapping("/clear")
    public R<String> clearCart(@RequestParam String openid) {
        try {
            if (openid == null || openid.trim().isEmpty()) {
                return R.error("openid不能为空");
            }

            boolean success = cartService.clearCart(openid);
            if (success) {
                log.info("清空购物车成功，用户: {}", openid);
                return R.ok("清空成功");
            } else {
                return R.error("清空失败");
            }
        } catch (Exception e) {
            log.error("清空购物车失败", e);
            return R.error("清空失败: " + e.getMessage());
        }
    }

    /**
     * 获取购物车商品数量
     */
    @GetMapping("/count")
    public R<Integer> getCartCount(@RequestParam String openid) {
        try {
            if (openid == null || openid.trim().isEmpty()) {
                return R.error("openid不能为空");
            }

            Integer count = cartService.getCartCount(openid);
            return R.ok(count);
        } catch (Exception e) {
            log.error("获取购物车数量失败", e);
            return R.error("获取购物车数量失败: " + e.getMessage());
        }
    }

    /**
     * 获取选中商品的总金额
     */
    @GetMapping("/selectedTotal")
    public R<Integer> getSelectedTotal(@RequestParam String openid) {
        try {
            if (openid == null || openid.trim().isEmpty()) {
                return R.error("openid不能为空");
            }

            Integer totalAmount = cartService.getSelectedTotalAmount(openid);
            return R.ok(totalAmount);
        } catch (Exception e) {
            log.error("计算选中商品总金额失败", e);
            return R.error("计算总金额失败: " + e.getMessage());
        }
    }

    /**
     * 购物车结算（创建订单）
     */
    @PostMapping("/checkout")
    public R<String> checkout(@RequestBody Map<String, Object> params) {
        try {
            String openid = (String) params.get("openid");
            String name = (String) params.get("name");
            String phone = (String) params.get("phone");
            String address = (String) params.get("address");
            String remark = (String) params.get("remark");

            // 参数校验
            if (openid == null || openid.trim().isEmpty()) {
                return R.error("openid不能为空");
            }
            if (name == null || name.trim().isEmpty()) {
                return R.error("收货人姓名不能为空");
            }
            if (phone == null || phone.trim().isEmpty()) {
                return R.error("收货人手机号不能为空");
            }
            if (address == null || address.trim().isEmpty()) {
                return R.error("收货地址不能为空");
            }

            // 校验手机号格式
            if (!phone.matches("^1[3-9]\\d{9}$")) {
                return R.error("手机号格式不正确");
            }

            // 获取选中的购物车商品
            List<Cart> selectedCarts = cartService.getCartByOpenid(openid)
                    .stream()
                    .filter(Cart::getSelected)
                    .collect(java.util.stream.Collectors.toList());

            if (selectedCarts.isEmpty()) {
                return R.error("请选择要结算的商品");
            }

            // 这里可以调用订单服务创建订单
            // 为了简化，这里只返回成功信息
            log.info("购物车结算，用户: {}, 选中商品数量: {}", openid, selectedCarts.size());
            return R.ok("结算成功，请前往订单页面查看");

        } catch (Exception e) {
            log.error("购物车结算失败", e);
            return R.error("结算失败: " + e.getMessage());
        }
    }
} 