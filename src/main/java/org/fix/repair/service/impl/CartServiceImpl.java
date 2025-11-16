package org.fix.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.fix.repair.common.R;
import org.fix.repair.entity.Cart;
import org.fix.repair.entity.books;
import org.fix.repair.mapper.CartMapper;
import org.fix.repair.service.BooksService;
import org.fix.repair.service.CartService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 购物车服务实现类
 */
@Slf4j
@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {

    private final BooksService booksService;

    public CartServiceImpl(BooksService booksService) {
        this.booksService = booksService;
    }

    @Override
    public R<String> addToCart(Map<String, Object> cartInfo) {
        try {
            String openid = (String) cartInfo.get("openid");
            Long bookId = Long.valueOf(cartInfo.get("bookId").toString());
            Integer quantity = Integer.valueOf(cartInfo.get("quantity").toString());

            if (openid == null || openid.trim().isEmpty()) {
                return R.error("用户openid不能为空");
            }
            if (bookId == null) {
                return R.error("书籍ID不能为空");
            }
            if (quantity == null || quantity <= 0) {
                return R.error("数量必须大于0");
            }

            // 检查书籍是否存在
            books book = booksService.getBook(bookId);
            if (book == null) {
                return R.error("书籍不存在");
            }

            // 检查库存
            if (book.getStock() < quantity) {
                return R.error("库存不足，当前库存：" + book.getStock());
            }

            // 检查购物车中是否已存在该商品
            LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Cart::getOpenid, openid)
                   .eq(Cart::getBookId, bookId);
            Cart existingCart = this.getOne(wrapper);

            if (existingCart != null) {
                // 更新数量
                int newQuantity = existingCart.getQuantity() + quantity;
                if (book.getStock() < newQuantity) {
                    return R.error("库存不足，当前库存：" + book.getStock() + "，购物车已有：" + existingCart.getQuantity());
                }
                existingCart.setQuantity(newQuantity);
                existingCart.setUpdatedAt(new Date());
                this.updateById(existingCart);
            } else {
                // 新增购物车项
                Cart cart = new Cart();
                cart.setOpenid(openid);
                cart.setBookId(bookId);
                cart.setBookName(book.getBookName());
                cart.setPrice(book.getPrice());
                cart.setImageUrl(book.getImageurl());
                cart.setQuantity(quantity);
                cart.setSelected(true); // 默认选中
                cart.setCreatedAt(new Date());
                cart.setUpdatedAt(new Date());
                this.save(cart);
            }

            log.info("用户 {} 添加商品到购物车，书籍ID: {}, 数量: {}", openid, bookId, quantity);
            return R.ok("添加成功");

        } catch (NumberFormatException e) {
            return R.error("参数格式错误");
        } catch (Exception e) {
            log.error("添加到购物车失败", e);
            return R.error("添加失败：" + e.getMessage());
        }
    }

    @Override
    public R<String> updateQuantity(Long cartId, Integer quantity) {
        try {
            if (cartId == null) {
                return R.error("购物车ID不能为空");
            }
            if (quantity == null || quantity <= 0) {
                return R.error("数量必须大于0");
            }

            Cart cart = this.getById(cartId);
            if (cart == null) {
                return R.error("购物车项不存在");
            }

            // 检查库存
            books book = booksService.getBook(cart.getBookId());
            if (book == null) {
                return R.error("商品已下架");
            }
            if (book.getStock() < quantity) {
                return R.error("库存不足，当前库存：" + book.getStock());
            }

            cart.setQuantity(quantity);
            cart.setUpdatedAt(new Date());
            this.updateById(cart);

            log.info("更新购物车数量，购物车ID: {}, 新数量: {}", cartId, quantity);
            return R.ok("更新成功");

        } catch (Exception e) {
            log.error("更新购物车数量失败", e);
            return R.error("更新失败：" + e.getMessage());
        }
    }

    @Override
    public R<String> removeFromCart(Long cartId) {
        try {
            if (cartId == null) {
                return R.error("购物车ID不能为空");
            }

            boolean removed = this.removeById(cartId);
            if (removed) {
                log.info("删除购物车项，ID: {}", cartId);
                return R.ok("删除成功");
            } else {
                return R.error("购物车项不存在");
            }

        } catch (Exception e) {
            log.error("删除购物车项失败", e);
            return R.error("删除失败：" + e.getMessage());
        }
    }

    @Override
    public R<String> clearCart(String openid) {
        try {
            if (openid == null || openid.trim().isEmpty()) {
                return R.error("用户openid不能为空");
            }

            LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Cart::getOpenid, openid);
            boolean removed = this.remove(wrapper);

            log.info("清空用户购物车，openid: {}", openid);
            return R.ok("清空成功");

        } catch (Exception e) {
            log.error("清空购物车失败", e);
            return R.error("清空失败：" + e.getMessage());
        }
    }

    @Override
    public R<List<Cart>> getCartList(String openid) {
        try {
            if (openid == null || openid.trim().isEmpty()) {
                return R.error("用户openid不能为空");
            }

            LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Cart::getOpenid, openid)
                   .orderByDesc(Cart::getUpdatedAt);
            List<Cart> cartList = this.list(wrapper);

            // 检查商品是否还存在，价格是否有变化
            for (Cart cart : cartList) {
                books book = booksService.getBook(cart.getBookId());
                if (book == null) {
                    // 商品已下架，标记为不可选
                    cart.setSelected(false);
                    cart.setBookName(cart.getBookName() + "（已下架）");
                } else {
                    // 更新商品信息（价格可能有变化）
                    cart.setBookName(book.getBookName());
                    cart.setPrice(book.getDiscountPrice() != null ? book.getDiscountPrice() : book.getPrice());
                    cart.setImageUrl(book.getImageurl());
                }
            }

            return R.ok(cartList);

        } catch (Exception e) {
            log.error("获取购物车列表失败", e);
            return R.error("获取失败：" + e.getMessage());
        }
    }

    @Override
    public R<String> batchSelectItems(List<Long> cartIds, Boolean selected) {
        try {
            if (cartIds == null || cartIds.isEmpty()) {
                return R.error("购物车ID列表不能为空");
            }
            if (selected == null) {
                return R.error("选中状态不能为空");
            }

            LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(Cart::getId, cartIds);
            
            List<Cart> carts = this.list(wrapper);
            for (Cart cart : carts) {
                cart.setSelected(selected);
                cart.setUpdatedAt(new Date());
            }
            
            this.updateBatchById(carts);

            log.info("批量更新购物车选中状态，ID列表: {}, 选中状态: {}", cartIds, selected);
            return R.ok("更新成功");

        } catch (Exception e) {
            log.error("批量更新购物车选中状态失败", e);
            return R.error("更新失败：" + e.getMessage());
        }
    }

    @Override
    public R<Integer> getSelectedCount(String openid) {
        try {
            if (openid == null || openid.trim().isEmpty()) {
                return R.error("用户openid不能为空");
            }

            LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Cart::getOpenid, openid)
                   .eq(Cart::getSelected, true);
            long count = this.count(wrapper);

            return R.ok((int) count);

        } catch (Exception e) {
            log.error("获取选中商品数量失败", e);
            return R.error("获取失败：" + e.getMessage());
        }
    }
}
