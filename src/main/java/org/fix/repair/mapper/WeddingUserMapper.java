package org.fix.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.fix.repair.entity.WeddingUser;

import java.util.Map;


/**
* @author admin
* @description 针对表【wedding_user(婚礼小程序用户信息表)】的数据库操作Mapper
* @createDate 2025-07-30 14:02:18
* @Entity com.hl.happy.entity.WeddingUser
*/
public interface WeddingUserMapper extends BaseMapper<WeddingUser> {

    void findByPhoneAndPassword(Map<String, Object> userInfo);
}




