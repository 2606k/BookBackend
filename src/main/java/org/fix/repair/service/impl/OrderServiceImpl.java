package org.fix.repair.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.fix.repair.entity.Order;
import org.fix.repair.mapper.OrderMapper;
import org.fix.repair.service.OrderService;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService{
}
