package org.fix.repair.controller;



import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.fix.repair.common.R;
import org.fix.repair.entity.WeddingUser;
import org.fix.repair.entity.appointments;
import org.fix.repair.mapper.WeddingUserMapper;
import org.fix.repair.mapper.AppointmentsMapper;
import org.fix.repair.service.WeddingUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户信息控制器
 */
@Controller
@RequestMapping("/user")
public class WeddingUserController {

    private final WeddingUserMapper userMapper;
    private final WeddingUserService userService;
    private final AppointmentsMapper appointmentsMapper;

    public WeddingUserController(WeddingUserMapper userMapper, WeddingUserService userService, AppointmentsMapper appointmentsMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.appointmentsMapper = appointmentsMapper;
    }



    /**
     * 保存用户信息
     * @param userInfo 用户信息
     * @return 保存结果
     */
    @PostMapping("/register")
    @ResponseBody
    public R<Long> saveUserInfo(@RequestBody Map<String, Object> userInfo) {
        try {
            WeddingUser user = new WeddingUser();
            user.setUsername((String) userInfo.get("userName"));
            user.setAvatarUrl((String) userInfo.get("avatarUrl"));
            user.setPhone((String) userInfo.get("phone"));
            user.setPassword((String) userInfo.get("password"));
            user.setCreatedat(new java.util.Date());
            userMapper.insert(user);
            return R.ok(user.getId());
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("保存用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 用户登录
     * @param userInfo 登录信息
     * @return 用户信息
     */
    @PostMapping("/checklogin")
    @ResponseBody
    public R<WeddingUser> login(@RequestBody Map<String, Object> userInfo) {
        try {
            LambdaQueryWrapper<WeddingUser> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(WeddingUser::getPhone, userInfo.get("phone"))
                    .eq(WeddingUser::getPassword, userInfo.get("password"));
            WeddingUser user= userMapper.selectOne(queryWrapper);
            if (user == null) {
                return R.error("用户不存在");
            }
            return R.ok(user);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error("用户登录失败: " + e.getMessage());
        }
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login"; // Maps to login.html in templates
    }

    // 后台管理主页
    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    // 预约记录查看页面
    @GetMapping("/appointments")
    public String appointmentsPage() {
        return "appointments/view";
    }



}