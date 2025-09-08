package org.fix.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.fix.repair.entity.brands;

@Mapper
public interface BrandsMapper<O> extends BaseMapper<brands> {
}
