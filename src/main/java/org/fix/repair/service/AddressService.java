package org.fix.repair.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.fix.repair.common.R;
import org.fix.repair.entity.Address;

import java.util.Map;

public interface AddressService extends IService<Address> {
    R<String> addAddress(Map<String, Object> addressInfo);
}
