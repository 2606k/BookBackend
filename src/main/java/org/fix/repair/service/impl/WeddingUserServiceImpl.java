package org.fix.repair.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.fix.repair.entity.WeddingUser;
import org.fix.repair.mapper.WeddingUserMapper;
import org.fix.repair.service.WeddingUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;


/**
* @author admin
* @description 针对表【wedding_user(婚礼小程序用户信息表)】的数据库操作Service实现
* @createDate 2025-07-30 14:02:18
*/
@Service
@RequiredArgsConstructor
public class WeddingUserServiceImpl extends ServiceImpl<WeddingUserMapper, WeddingUser>
    implements WeddingUserService {


}




