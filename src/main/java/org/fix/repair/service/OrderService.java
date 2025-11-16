package org.fix.repair.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.fix.repair.entity.Order;

public interface OrderService extends IService<Order> {
    Order getUserOpenidByOutTradeNo(String outTradeNo);

    void markAsCompleted(String outTradeNo);
}
