package org.fix.repair.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.fix.repair.entity.brands;
import org.fix.repair.mapper.BrandsMapper;
import org.fix.repair.service.BrandsService;
import org.springframework.stereotype.Service;

@Service
public class BrandsServiceImpl extends ServiceImpl<BrandsMapper<O>, brands> implements BrandsService {
}
