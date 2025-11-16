package org.fix.repair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.fix.repair.common.R;
import org.fix.repair.entity.Address;
import org.fix.repair.mapper.AddressMapper;
import org.fix.repair.service.AddressService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/address")
public class AddressController {
    private final AddressService addressService;

    private final AddressMapper addressMapper;

    public AddressController(AddressService addressService, AddressMapper addressMapper) {
        this.addressService = addressService;
        this.addressMapper = addressMapper;
    }

    @RequestMapping("/list")
    public R<List<Address>> list(@RequestBody Map<String, Object> addressInfo) {
        if (addressInfo.get("openid") == null){
            Object phone = addressInfo.get("phone");
            Object address = addressInfo.get("address");
            LambdaQueryWrapper<Address> queryWrapper = new LambdaQueryWrapper<>();
            // 只有当phone不为空时才添加条件
            if (phone != null && !phone.toString().trim().isEmpty()) {
                queryWrapper.eq(Address::getPhone, phone);
            }

            // 只有当address不为空时才添加条件
            if (address != null && !address.toString().trim().isEmpty()) {
                queryWrapper.like(Address::getAddress, address);
            };
            return R.ok(addressService.list(queryWrapper));
        }
        LambdaQueryWrapper<Address> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Address::getOpenid, addressInfo.get("openid"));
        return R.ok(addressMapper.selectList(queryWrapper));
    }

    @RequestMapping("/add")
    public R<String> add(@RequestBody Map<String, Object> addressInfo) {
        return addressService.addAddress(addressInfo);
    }

    @RequestMapping("/delete")
    public boolean delete(@RequestBody Integer id) {
        return addressService.removeById(id);
    }

    @RequestMapping("/update")
    public boolean update(@RequestBody Map<String, Object> addressInfo) {
        return addressService.updateAddress(addressInfo);
    }
}
