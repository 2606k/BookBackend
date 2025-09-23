package org.fix.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.fix.repair.entity.Cart;
import org.apache.ibatis.annotations.Mapper;

/**
 * 购物车Mapper接口
 */
@Mapper
public interface CartMapper extends BaseMapper<Cart> {
}
