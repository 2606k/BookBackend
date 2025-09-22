package org.fix.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.fix.repair.entity.Cart;
import org.fix.repair.entity.books;
import org.fix.repair.mapper.CartMapper;
import org.fix.repair.service.BooksService;
import org.fix.repair.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 购物车Service实现类
 */
@Slf4j
@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {

    @Autowired
    private BooksService booksService;

    @Override
    public List<Cart> getCartByOpenid(String openid) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getOpenid, openid)
               .orderByDesc(Cart::getCreatedAt);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addToCart(String openid, Long bookId, Integer quantity) {
        try {
            // 检查商品是否存在
            books book = booksService.getBook(bookId);
            if (book == null) {
                log.error("商品不存在，bookId: {}", bookId);
                return false;
            }

            // 检查库存
            if (book.getStock() < quantity) {
                log.error("商品库存不足，bookId: {}, 需要: {}, 库存: {}", bookId, quantity, book.getStock());
                return false;
            }

            // 检查购物车中是否已存在该商品
            LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Cart::getOpenid, openid)
                   .eq(Cart::getBookId, bookId);
            Cart existingCart = getOne(wrapper);

            if (existingCart != null) {
                // 商品已存在，更新数量
                int newQuantity = existingCart.getQuantity() + quantity;
                if (newQuantity > book.getStock()) {
                    log.error("购物车商品数量超过库存，bookId: {}, 新数量: {}, 库存: {}", bookId, newQuantity, book.getStock());
                    return false;
                }
                existingCart.setQuantity(newQuantity);
                existingCart.setUpdatedAt(new Date());
                return updateById(existingCart);
            } else {
                // 新增购物车项
                Cart cart = new Cart();
                cart.setOpenid(openid);
                cart.setBookId(bookId);
                cart.setBookName(book.getBookName());
                cart.setPrice(book.getPrice());
                cart.setQuantity(quantity);
                cart.setSelected(true); // 默认选中
                cart.setCreatedAt(new Date());
                cart.setUpdatedAt(new Date());
                return save(cart);
            }
        } catch (Exception e) {
            log.error("添加购物车失败", e);
            return false;
        }
    }

    @Override
    public boolean updateCartQuantity(Long cartId, Integer quantity) {
        try {
            Cart cart = getById(cartId);
            if (cart == null) {
                log.error("购物车项不存在，cartId: {}", cartId);
                return false;
            }

            // 检查库存
            books book = booksService.getBook(cart.getBookId());
            if (book == null || book.getStock() < quantity) {
                log.error("商品库存不足，bookId: {}, 需要: {}, 库存: {}", 
                         cart.getBookId(), quantity, book != null ? book.getStock() : 0);
                return false;
            }

            cart.setQuantity(quantity);
            cart.setUpdatedAt(new Date());
            return updateById(cart);
        } catch (Exception e) {
            log.error("更新购物车数量失败", e);
            return false;
        }
    }

    @Override
    public boolean updateCartSelected(Long cartId, Boolean selected) {
        try {
            LambdaUpdateWrapper<Cart> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(Cart::getId, cartId)
                   .set(Cart::getSelected, selected)
                   .set(Cart::getUpdatedAt, new Date());
            return update(wrapper);
        } catch (Exception e) {
            log.error("更新购物车选中状态失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCartSelectedBatch(String openid, List<Long> cartIds, Boolean selected) {
        try {
            LambdaUpdateWrapper<Cart> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(Cart::getOpenid, openid)
                   .in(Cart::getId, cartIds)
                   .set(Cart::getSelected, selected)
                   .set(Cart::getUpdatedAt, new Date());
            return update(wrapper);
        } catch (Exception e) {
            log.error("批量更新购物车选中状态失败", e);
            return false;
        }
    }

    @Override
    public boolean removeFromCart(Long cartId) {
        try {
            return removeById(cartId);
        } catch (Exception e) {
            log.error("删除购物车项失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeFromCartBatch(String openid, List<Long> cartIds) {
        try {
            LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Cart::getOpenid, openid)
                   .in(Cart::getId, cartIds);
            return remove(wrapper);
        } catch (Exception e) {
            log.error("批量删除购物车项失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean clearCart(String openid) {
        try {
            LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Cart::getOpenid, openid);
            return remove(wrapper);
        } catch (Exception e) {
            log.error("清空购物车失败", e);
            return false;
        }
    }

    @Override
    public Integer getCartCount(String openid) {
        try {
            LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Cart::getOpenid, openid);
            return Math.toIntExact(count(wrapper));
        } catch (Exception e) {
            log.error("获取购物车数量失败", e);
            return 0;
        }
    }

    @Override
    public Integer getSelectedTotalAmount(String openid) {
        try {
            LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Cart::getOpenid, openid)
                   .eq(Cart::getSelected, true);
            List<Cart> selectedCarts = list(wrapper);
            
            return selectedCarts.stream()
                    .mapToInt(cart -> cart.getPrice() * cart.getQuantity())
                    .sum();
        } catch (Exception e) {
            log.error("计算选中商品总金额失败", e);
            return 0;
        }
    }
} 