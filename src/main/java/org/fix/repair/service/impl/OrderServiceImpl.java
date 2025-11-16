package org.fix.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.fix.repair.entity.Order;
import org.fix.repair.mapper.OrderMapper;
import org.fix.repair.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService{

    @Autowired
    private OrderMapper orderMapper;

    @Override
    public Order getUserOpenidByOutTradeNo(String outTradeNo) {
        Order order = orderMapper.selectOne(new QueryWrapper<Order>().eq("out_trade_no", outTradeNo));
        return order;
    }

    @Override
    public void markAsCompleted(String outTradeNo) {
        Order order = new Order();
        order.setStatus("3");

        orderMapper.update(order, new QueryWrapper<Order>().eq("out_trade_no", outTradeNo));
    }


}
