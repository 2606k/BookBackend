package org.fix.repair.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.fix.repair.entity.categories;
import org.fix.repair.mapper.CategoriesMapper;
import org.fix.repair.service.CategoriesService;
import org.springframework.stereotype.Service;

@Service
public class CategoriesServiceImpl extends ServiceImpl<CategoriesMapper, categories> implements CategoriesService{
}
