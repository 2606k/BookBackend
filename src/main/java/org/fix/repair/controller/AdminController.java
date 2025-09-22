package org.fix.repair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fix.repair.common.R;
import org.fix.repair.entity.WeddingUser;
import org.fix.repair.entity.books;
import org.fix.repair.mapper.WeddingUserMapper;
import org.fix.repair.service.WeddingUserService;
import org.fix.repair.service.BooksService;
import org.fix.repair.service.CategoriesService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 管理端页面控制器
 */
@Slf4j
@Controller
@RequestMapping("/admin")
@AllArgsConstructor
public class AdminController {

    private final WeddingUserService userService;
    private final WeddingUserMapper userMapper;
    private final BooksService booksService;
    private final CategoriesService categoriesService;

    /**
     * 管理员注册
     */
    @PostMapping("/register")
    @ResponseBody
    public R<Long> adminRegister(@RequestBody Map<String, Object> userInfo) {
        try {
            // 检查手机号是否已存在
            LambdaQueryWrapper<WeddingUser> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(WeddingUser::getPhone, userInfo.get("phone"));
            WeddingUser existingUser = userMapper.selectOne(queryWrapper);
            if (existingUser != null) {
                return R.error("该手机号已被注册");
            }

            WeddingUser user = new WeddingUser();
            user.setUsername((String) userInfo.get("userName"));
            user.setPhone((String) userInfo.get("phone"));
            user.setPassword((String) userInfo.get("password"));
            user.setCreatedat(new java.util.Date());
            userMapper.insert(user);
            log.info("管理员注册成功: {}", user.getPhone());
            return R.ok(user.getId());
        } catch (Exception e) {
            log.error("管理员注册失败", e);
            return R.error("注册失败: " + e.getMessage());
        }
    }

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    @ResponseBody
    public R<WeddingUser> adminLogin(@RequestBody Map<String, Object> userInfo) {
        try {
            LambdaQueryWrapper<WeddingUser> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(WeddingUser::getPhone, userInfo.get("phone"))
                    .eq(WeddingUser::getPassword, userInfo.get("password"));
            WeddingUser user = userMapper.selectOne(queryWrapper);
            if (user == null) {
                return R.error("手机号或密码错误");
            }
            log.info("管理员登录成功: {}", user.getPhone());
            return R.ok(user);
        } catch (Exception e) {
            log.error("管理员登录失败", e);
            return R.error("登录失败: " + e.getMessage());
        }
    }

    /**
     * 文件上传接口
     */
    @PostMapping("/api/upload")
    @ResponseBody
    public R<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return R.error("上传文件不能为空");
            }

            // 检查文件类型
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return R.error("文件名不能为空");
            }

            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (!fileExtension.matches("\\.(jpg|jpeg|png|gif|bmp)$")) {
                return R.error("只支持图片文件格式");
            }

            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString() + fileExtension;
            
            // 创建上传目录
            String uploadDir = "uploads/images/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 保存文件
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            Map<String, String> result = new HashMap<>();
            result.put("url", "/" + uploadDir + fileName);
            result.put("filename", fileName);
            
            log.info("文件上传成功: {}", fileName);
            return R.ok(result);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return R.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取管理端统计数据
     */
    @GetMapping("/api/statistics")
    @ResponseBody
    public R<Map<String, Object>> getDashboardStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();
            
            // 用户统计
            long totalUsers = userService.count();
            statistics.put("totalUsers", totalUsers);
            
            // 书籍统计
            List<books> allBooks = booksService.list();
            statistics.put("totalBooks", allBooks.size());
            statistics.put("totalStock", allBooks.stream().mapToInt(books::getStock).sum());
            statistics.put("lowStockCount", allBooks.stream()
                    .filter(book -> book.getStock() <= 10)
                    .count());
            
            // 分类统计
            long totalCategories = categoriesService.count();
            statistics.put("totalCategories", totalCategories);
            
            // 模拟其他统计数据（订单相关）
            statistics.put("totalOrders", 1234);
            statistics.put("totalRevenue", 89567.0);
            statistics.put("activeUsers", 2890);
            
            log.info("获取管理端统计数据成功");
            return R.ok(statistics);
        } catch (Exception e) {
            log.error("获取管理端统计数据失败", e);
            return R.error("获取统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户列表（用于用户管理）
     */
    @GetMapping("/api/users")
    @ResponseBody
    public R<List<WeddingUser>> getUserList() {
        try {
            List<WeddingUser> users = userService.list();
            log.info("获取用户列表成功，用户数量: {}", users.size());
            return R.ok(users);
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return R.error("获取用户列表失败: " + e.getMessage());
        }
    }

    /**
     * 删除用户
     */
    @PostMapping("/api/users/delete/{userId}")
    @ResponseBody
    public R<String> deleteUser(@PathVariable Long userId) {
        try {
            WeddingUser user = userService.getById(userId);
            if (user == null) {
                return R.error("用户不存在");
            }
            
            boolean success = userService.removeById(userId);
            if (success) {
                log.info("删除用户成功: {}", user.getUsername());
                return R.ok("删除用户成功");
            } else {
                return R.error("删除用户失败");
            }
        } catch (Exception e) {
            log.error("删除用户失败", e);
            return R.error("删除用户失败: " + e.getMessage());
        }
    }

    /**
     * 搜索用户
     */
    @GetMapping("/api/users/search")
    @ResponseBody
    public R<List<WeddingUser>> searchUsers(@RequestParam String keyword) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return R.error("搜索关键词不能为空");
            }
            
            LambdaQueryWrapper<WeddingUser> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.like(WeddingUser::getUsername, keyword.trim())
                    .or()
                    .like(WeddingUser::getPhone, keyword.trim());
            
            List<WeddingUser> users = userService.list(queryWrapper);
            log.info("搜索用户: {}，结果数量: {}", keyword, users.size());
            return R.ok(users);
        } catch (Exception e) {
            log.error("搜索用户失败", e);
            return R.error("搜索用户失败: " + e.getMessage());
        }
    }

    /**
     * 管理端控制台
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        log.info("访问管理端控制台");
        return "admin-dashboard";
    }

    /**
     * 书籍管理页面
     */
    @GetMapping("/books")
    public String books(Model model) {
        log.info("访问书籍管理页面");
        return "books-manage";
    }

    /**
     * 分类管理页面
     */
    @GetMapping("/categories")
    public String categories(Model model) {
        log.info("访问分类管理页面");
        return "categories-manage";
    }

    /**
     * 订单管理页面
     */
    @GetMapping("/orders")
    public String orders(Model model) {
        log.info("访问订单管理页面");
        return "order-manage";
    }

    /**
     * 用户管理页面
     */
    @GetMapping("/users")
    public String users(Model model) {
        log.info("访问用户管理页面");
        return "users-manage";
    }

    /**
     * 数据统计页面
     */
    @GetMapping("/statistics")
    public String statistics(Model model) {
        log.info("访问数据统计页面");
        return "statistics";
    }

    /**
     * 系统设置页面
     */
    @GetMapping("/settings")
    public String settings(Model model) {
        log.info("访问系统设置页面");
        return "settings";
    }

    /**
     * 登录页面
     */
    @GetMapping("/login")
    public String login() {
        return "admin-login";
    }

    /**
     * 退出登录
     */
    @GetMapping("/logout")
    public String logout() {
        log.info("管理员退出登录");
        return "redirect:/admin/login";
    }
} 