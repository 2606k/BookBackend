package org.fix.repair.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.fix.repair.common.R;
import org.fix.repair.entity.Address;
import org.fix.repair.mapper.AddressMapper;
import org.fix.repair.service.AddressService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements AddressService {
    @Override
    public R<String> addAddress(Map<String, Object> addressInfo) {
        Address address = new Address();
        address.setOpenid((String) addressInfo.get("openid"));
        address.setName((String) addressInfo.get("name"));
        address.setPhone((String) addressInfo.get("phone"));
        address.setAddress((String) addressInfo.get("address"));
        if (this.save(address)) {
            return R.ok("添加成功");
        }else {
            return R.error("添加失败");
        }
    }
}
