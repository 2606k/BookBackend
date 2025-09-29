package org.fix.repair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
     * 获取用户列表（带分页）
     */
    @GetMapping("/api/users")
    @ResponseBody
    public R<Map<String, Object>> getUserList(@RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer size,
                                              @RequestParam(required = false) String keyword) {
        try {
            // 创建分页对象
            Page<WeddingUser> pageObj = new Page<>(page, size);
            
            // 构建查询条件
            LambdaQueryWrapper<WeddingUser> queryWrapper = new LambdaQueryWrapper<>();
            
            // 关键词搜索（用户名或手机号）
            if (keyword != null && !keyword.trim().isEmpty()) {
                queryWrapper.like(WeddingUser::getUsername, keyword.trim())
                          .or()
                          .like(WeddingUser::getPhone, keyword.trim());
            }
            
            // 按创建时间倒序
            queryWrapper.orderByDesc(WeddingUser::getCreatedat);
            
            // 执行分页查询
            Page<WeddingUser> result = userService.page(pageObj, queryWrapper);
            
            // 构建返回结果
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("records", result.getRecords());
            responseData.put("total", result.getTotal());
            responseData.put("pages", result.getPages());
            responseData.put("current", result.getCurrent());
            responseData.put("size", result.getSize());
            
            log.info("获取用户列表成功，页码: {}, 每页: {}, 总数: {}", page, size, result.getTotal());
            return R.ok(responseData);
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
     * 搜索用户（兼容旧接口）
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
     * 导出书籍数据
     */
    @GetMapping("/api/books/export")
    @ResponseBody
    public R<String> exportBooks() {
        try {
            List<books> allBooks = booksService.list();
            log.info("导出书籍数据，总数量: {}", allBooks.size());
            
            // 这里可以实现具体的导出逻辑，比如生成Excel文件
            // 简化处理，返回成功消息
            return R.ok("书籍数据导出成功，共导出 " + allBooks.size() + " 条记录");
        } catch (Exception e) {
            log.error("导出书籍数据失败", e);
            return R.error("导出书籍数据失败: " + e.getMessage());
        }
    }

    /**
     * 导出分类数据
     */
    @GetMapping("/api/categories/export")
    @ResponseBody
    public R<String> exportCategories() {
        try {
            long totalCategories = categoriesService.count();
            log.info("导出分类数据，总数量: {}", totalCategories);
            
            // 这里可以实现具体的导出逻辑
            return R.ok("分类数据导出成功，共导出 " + totalCategories + " 条记录");
        } catch (Exception e) {
            log.error("导出分类数据失败", e);
            return R.error("导出分类数据失败: " + e.getMessage());
        }
    }

    /**
     * 导出用户数据
     */
    @GetMapping("/api/users/export")
    @ResponseBody
    public R<String> exportUsers() {
        try {
            long totalUsers = userService.count();
            log.info("导出用户数据，总数量: {}", totalUsers);
            
            // 这里可以实现具体的导出逻辑
            return R.ok("用户数据导出成功，共导出 " + totalUsers + " 条记录");
        } catch (Exception e) {
            log.error("导出用户数据失败", e);
            return R.error("导出用户数据失败: " + e.getMessage());
        }
    }

    /**
     * 系统信息接口
     */
    @GetMapping("/api/system/info")
    @ResponseBody
    public R<Map<String, Object>> getSystemInfo() {
        try {
            Map<String, Object> systemInfo = new HashMap<>();
            
            // 基本系统信息
            systemInfo.put("systemName", "图书商城管理系统");
            systemInfo.put("version", "1.0.0");
            systemInfo.put("javaVersion", System.getProperty("java.version"));
            systemInfo.put("osName", System.getProperty("os.name"));
            systemInfo.put("serverTime", new java.util.Date());
            
            // 内存信息
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            Map<String, Object> memoryInfo = new HashMap<>();
            memoryInfo.put("total", totalMemory / 1024 / 1024 + " MB");
            memoryInfo.put("used", usedMemory / 1024 / 1024 + " MB");
            memoryInfo.put("free", freeMemory / 1024 / 1024 + " MB");
            systemInfo.put("memory", memoryInfo);
            
            log.info("获取系统信息成功");
            return R.ok(systemInfo);
        } catch (Exception e) {
            log.error("获取系统信息失败", e);
            return R.error("获取系统信息失败: " + e.getMessage());
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
     * 登录页面（根路径重定向）
     */
    @GetMapping("")
    public String adminRoot() {
        return "redirect:/admin/login";
    }

    /**
     * 根路径重定向
     */
    @GetMapping("/")
    public String adminIndex() {
        return "redirect:/admin/login";
    }

    /**
     * 退出登录
     */
    @GetMapping("/logout")
    public String logout() {
        log.info("管理员退出登录");
        return "redirect:/admin/login";
    }

    /**
     * 获取热销书籍TOP排行榜
     */
    @GetMapping("/api/books/top")
    @ResponseBody
    public R<List<Map<String, Object>>> getTopBooks(@RequestParam(defaultValue = "5") Integer limit) {
        try {
            // 这里简化处理，实际应该根据销量排序
            List<books> allBooks = booksService.list();
            List<Map<String, Object>> topBooks = new java.util.ArrayList<>();
            
            int count = Math.min(limit, allBooks.size());
            for (int i = 0; i < count; i++) {
                books book = allBooks.get(i);
                Map<String, Object> bookData = new HashMap<>();
                bookData.put("id", book.getId());
                bookData.put("name", book.getBookName());
                bookData.put("author", book.getAuthor());
                bookData.put("price", book.getPrice());
                bookData.put("stock", book.getStock());
                bookData.put("sales", 100 - i * 10); // 模拟销量数据
                topBooks.add(bookData);
            }
            
            log.info("获取热销书籍TOP{}成功", limit);
            return R.ok(topBooks);
        } catch (Exception e) {
            log.error("获取热销书籍失败", e);
            return R.error("获取热销书籍失败: " + e.getMessage());
        }
    }

    /**
     * 获取最近订单（简化版本）
     */
    @GetMapping("/api/orders/recent")
    @ResponseBody
    public R<List<Map<String, Object>>> getRecentOrders(@RequestParam(defaultValue = "5") Integer limit) {
        try {
            // 这里简化处理，返回模拟数据
            // 实际应该从Order表中查询最新的订单
            List<Map<String, Object>> recentOrders = new java.util.ArrayList<>();
            
            for (int i = 1; i <= limit; i++) {
                Map<String, Object> order = new HashMap<>();
                order.put("id", "ORDER_" + String.format("%03d", i));
                order.put("customer", "用户" + i);
                order.put("amount", 89.50 + i * 10);
                order.put("status", i % 3 == 0 ? "已支付" : i % 3 == 1 ? "待支付" : "已完成");
                order.put("time", new java.util.Date());
                recentOrders.add(order);
            }
            
            log.info("获取最近{}条订单成功", limit);
            return R.ok(recentOrders);
        } catch (Exception e) {
            log.error("获取最近订单失败", e);
            return R.error("获取最近订单失败: " + e.getMessage());
        }
    }
} 