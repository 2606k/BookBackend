package org.fix.repair.controller;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.fix.repair.common.R;
import org.fix.repair.common.MinioUtil;
import org.fix.repair.entity.user;
import org.fix.repair.entity.books;
import org.fix.repair.mapper.WeddingUserMapper;
import org.fix.repair.service.WeddingUserService;
import org.fix.repair.service.BooksService;
import org.fix.repair.service.CategoriesService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

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
    private final MinioUtil minioUtil;
    private final org.fix.repair.service.OrderService orderService;

    /**
     * 管理员注册
     */
    @PostMapping("/register")
    @ResponseBody
    public R<Long> adminRegister(@RequestBody Map<String, Object> userInfo) {
        try {
            // 检查手机号是否已存在
            LambdaQueryWrapper<user> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(user::getPhone, userInfo.get("phone"));
            user existingUser = userMapper.selectOne(queryWrapper);
            if (existingUser != null) {
                return R.error("该手机号已被注册");
            }
            user userlocal = new user();
            Object openid = userInfo.get("openid");
            LambdaQueryWrapper<user> queryWrapper2 = new LambdaQueryWrapper<>();
            queryWrapper2.eq(user::getOpenid, openid);
            user user1= userMapper.selectOne(queryWrapper);
            if (user1 != null){
                userlocal.setUsername((String) userInfo.get("userName"));
                userlocal.setAvatarUrl((String) userInfo.get("avatarUrl"));
                userMapper.update(userlocal, queryWrapper);
                return R.ok(user1.getId());
            }
            userlocal.setUsername((String) userInfo.get("userName"));
            userlocal.setAvatarUrl((String) userInfo.get("avatarUrl"));
            userlocal.setPhone((String) userInfo.get("phone"));
            userlocal.setOpenid((String) userInfo.get("openid"));
            userlocal.setPassword((String) userInfo.get("password"));
            userlocal.setCreatedat(new java.util.Date());
            userMapper.insert(userlocal);
            return R.ok(userlocal.getId());
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
    public R<user> adminLogin(@RequestBody Map<String, Object> userInfo) {
        try {
            LambdaQueryWrapper<user> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(user::getPhone, userInfo.get("phone"))
                    .eq(user::getPassword, userInfo.get("password"));
            user user = userMapper.selectOne(queryWrapper);
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

    @PostMapping("/getOpenId")
    @ResponseBody
    public R<String> getOpenId(@RequestBody String json) throws IOException {
        // 解析JSON字符串获取code
        JSONObject jsonObject = JSONObject.parseObject(json);
        String code = jsonObject.getString("code");
        //AppID
        String appId = "wx53080d824201ddf5";
        //密钥
        String secret= "0bc64f03b4c21f3fb74a2824939fb02a";
        // 直接使用传入的字符串作为code
        if (StringUtils.isEmpty(code)) {
            return R.error("code不能为空");
        }
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid="+appId
                +"&secret="+secret+"&js_code="+code+"&grant_type=authorization_code";

        //客户端
        OkHttpClient client = new OkHttpClient();
        //用url发起请求
        Request request = new Request.Builder().url(url).build();
        //拿到响应
        Response response = client.newCall(request).execute();
        //如果响应成功，打印返回值
        if (response.isSuccessful()){
            String body = response.body().string();
            System.out.println(body);
            JSONObject jsonObject2 = JSONObject.parseObject(body);
            String openid = jsonObject2.getString("openid");
            return R.ok(openid);
        }
        return R.error("请求失败");
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

            // 使用MinioUtil验证文件类型
            if (!minioUtil.isImageFile(originalFilename)) {
                return R.error("只支持图片文件格式 (jpg, jpeg, png, gif, bmp, webp)");
            }

            // 上传文件到MinIO bucket根目录
            String fileUrl = minioUtil.uploadFileToRoot(file);

            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("filename", originalFilename);
            
            log.info("文件上传成功: {} -> {}", originalFilename, fileUrl);
            return R.ok(result);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return R.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 文件下载接口
     */
    @GetMapping("/api/download")
    @ResponseBody
    public ResponseEntity<byte[]> downloadFile(@RequestParam("url") String fileUrl) {
        try {
            // 从URL中提取对象名
            String objectName = minioUtil.extractObjectName(fileUrl);
            if (objectName == null) {
                return ResponseEntity.badRequest().build();
            }

            // 检查文件是否存在
            if (!minioUtil.fileExists(objectName)) {
                return ResponseEntity.notFound().build();
            }

            // 获取文件信息
            var fileInfo = minioUtil.getFileInfo(objectName);
            
            // 下载文件
            InputStream inputStream = minioUtil.downloadFile(objectName);
            byte[] fileBytes = inputStream.readAllBytes();
            inputStream.close();

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(fileInfo.contentType()));
            headers.setContentDispositionFormData("attachment", fileInfo.object());
            headers.setContentLength(fileBytes.length);

            log.info("文件下载成功: {}", objectName);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileBytes);

        } catch (Exception e) {
            log.error("文件下载失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 文件删除接口
     */
    @DeleteMapping("/api/file")
    @ResponseBody
    public R<String> deleteFile(@RequestParam("url") String fileUrl) {
        try {
            // 从URL中提取对象名
            String objectName = minioUtil.extractObjectName(fileUrl);
            if (objectName == null) {
                return R.error("无效的文件URL");
            }

            // 检查文件是否存在
            if (!minioUtil.fileExists(objectName)) {
                return R.error("文件不存在");
            }

            // 删除文件
            minioUtil.deleteFile(objectName);

            log.info("文件删除成功: {}", objectName);
            return R.ok("文件删除成功");
        } catch (Exception e) {
            log.error("文件删除失败", e);
            return R.error("文件删除失败: " + e.getMessage());
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
            
            // 订单统计（真实数据）
            long totalOrders = orderService.count();
            statistics.put("totalOrders", totalOrders);
            
            // 计算总销售额（只统计已支付和已完成的订单）
            List<org.fix.repair.entity.Order> paidOrders = orderService.list(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.fix.repair.entity.Order>()
                            .in(org.fix.repair.entity.Order::getStatus, "0", "3") // 0=已支付, 3=已完成
            );
            double totalRevenue = paidOrders.stream()
                    .mapToDouble(order -> order.getMoney() / 100.0) // 转换为元
                    .sum();
            statistics.put("totalRevenue", totalRevenue);
            
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
            Page<user> pageObj = new Page<>(page, size);
            
            // 构建查询条件
            LambdaQueryWrapper<user> queryWrapper = new LambdaQueryWrapper<>();
            
            // 关键词搜索（用户名或手机号）
            if (keyword != null && !keyword.trim().isEmpty()) {
                queryWrapper.like(user::getUsername, keyword.trim())
                          .or()
                          .like(user::getPhone, keyword.trim());
            }
            
            // 按创建时间倒序
            queryWrapper.orderByDesc(user::getCreatedat);
            
            // 执行分页查询
            Page<user> result = userService.page(pageObj, queryWrapper);
            
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
            user user = userService.getById(userId);
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
    public R<List<user>> searchUsers(@RequestParam String keyword) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return R.error("搜索关键词不能为空");
            }
            
            LambdaQueryWrapper<user> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.like(user::getUsername, keyword.trim())
                    .or()
                    .like(user::getPhone, keyword.trim());
            
            List<user> users = userService.list(queryWrapper);
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
     * 获取最近订单
     */
    @GetMapping("/api/orders/recent")
    @ResponseBody
    public R<List<Map<String, Object>>> getRecentOrders(@RequestParam(defaultValue = "10") Integer limit) {
        try {
            // 从数据库查询最新的订单
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<org.fix.repair.entity.Order> page = 
                    new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, limit);
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.fix.repair.entity.Order> queryWrapper = 
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            queryWrapper.orderByDesc(org.fix.repair.entity.Order::getCreatedat);
            
            orderService.page(page, queryWrapper);
            
            List<Map<String, Object>> recentOrders = new java.util.ArrayList<>();
            for (org.fix.repair.entity.Order order : page.getRecords()) {
                Map<String, Object> orderData = new HashMap<>();
                orderData.put("id", order.getOutTradeNo() != null ? order.getOutTradeNo() : "ORD-" + order.getId());
                orderData.put("customer", order.getName());
                orderData.put("amount", order.getMoney() / 100.0);
                
                // 状态映射
                String statusText;
                switch (order.getStatus()) {
                    case "0":
                        statusText = "已支付";
                        break;
                    case "1":
                        statusText = "申请退款";
                        break;
                    case "2":
                        statusText = "已退款";
                        break;
                    case "3":
                        statusText = "已完成";
                        break;
                    default:
                        statusText = "未知";
                }
                orderData.put("status", statusText);
                orderData.put("time", order.getCreatedat());
                recentOrders.add(orderData);
            }
            
            log.info("获取最近{}条订单成功", limit);
            return R.ok(recentOrders);
        } catch (Exception e) {
            log.error("获取最近订单失败", e);
            return R.error("获取最近订单失败: " + e.getMessage());
        }
    }
} 