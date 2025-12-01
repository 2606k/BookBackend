package org.fix.repair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fix.repair.common.R;
import org.fix.repair.entity.books;
import org.fix.repair.entity.categories;
import org.fix.repair.service.BooksService;
import org.fix.repair.service.CategoriesService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 书籍管理API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/books")
@AllArgsConstructor
public class BooksController {

    private final BooksService booksService;
    private final CategoriesService categoriesService;

    /**
     * 获取书籍列表（带分页和搜索）
     */
    @GetMapping("/list")
    public R<Map<String, Object>> getBooksList(@RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer size,
                                               @RequestParam(required = false) String bookName,
                                               @RequestParam(required = false) Integer minPrice,
                                               @RequestParam(required = false) Integer maxPrice,
                                               @RequestParam(required = false) String stockStatus,
                                               @RequestParam(required = false) Integer isPinned,
                                               @RequestParam(required = false) Long categoryId,
                                               @RequestParam(defaultValue = "default") String sortType) {
        try {
            // 创建分页对象
            Page<books> pageObj = new Page<>(page, size);
            
            // 构建查询条件
            LambdaQueryWrapper<books> queryWrapper = new LambdaQueryWrapper<>();
            
            // 书名模糊查询
            if (bookName != null && !bookName.trim().isEmpty()) {
                queryWrapper.like(books::getBookName, bookName.trim());
            }
            
            // 价格范围查询（价格以分为单位）
            if (minPrice != null) {
                queryWrapper.ge(books::getPrice, minPrice * 100);
            }
            if (maxPrice != null) {
                queryWrapper.le(books::getPrice, maxPrice * 100);
            }
            if(isPinned != null){
                queryWrapper.eq(books::getIsPinned,isPinned);
            }
            
            // 分类查询
            if (categoryId != null) {
                queryWrapper.eq(books::getCategoryId, categoryId);
            }
            
            // 库存状态查询
            if (stockStatus != null && !stockStatus.trim().isEmpty()) {
                switch (stockStatus) {
                    case "inStock":
                        queryWrapper.gt(books::getStock, 0);
                        break;
                    case "lowStock":
                        queryWrapper.le(books::getStock, 10).gt(books::getStock, 0);
                        break;
                    case "outOfStock":
                        queryWrapper.eq(books::getStock, 0);
                        break;
                }
            }
            
            // 按置顶状态倒序，然后按创建时间倒序
//            queryWrapper.orderByDesc(books::getIsPinned)
//                       .orderByDesc(books::getCreatedat);
            // 根据sortType参数设置不同的排序逻辑
            switch (sortType) {
                case "newest": // 新书上架：只按最新时间排序
                    if (isPinned != null) {
                        queryWrapper.eq(books::getIsPinned, isPinned);
                    }
                    queryWrapper.orderByDesc(books::getCreatedat);
                    break;

                case "hot": // 热门推荐：只查询置顶书籍
                    queryWrapper.eq(books::getIsPinned, 1);
                    // 可以按置顶时间或创建时间排序
                    queryWrapper.orderByDesc(books::getCreatedat);
                    break;

                case "default": // 所有书籍：默认排序（置顶靠前，再按时间）
                default:
                    if (isPinned != null) {
                        queryWrapper.eq(books::getIsPinned, isPinned);
                    }
                    queryWrapper.orderByDesc(books::getIsPinned)
                            .orderByDesc(books::getCreatedat);
                    break;
            }
            
            // 执行分页查询
            Page<books> result = booksService.page(pageObj, queryWrapper);
            
            // 为每本书添加分类信息
            List<books> booksList = result.getRecords();
            for (books book : booksList) {
                if (book.getCategoryId() != null) {
                    categories category = categoriesService.getById(book.getCategoryId());
                    if (category != null) {
                        // 可以添加分类名称字段到书籍实体中，或者通过Map返回
                    }
                }
            }
            
            // 构建返回结果
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("records", booksList);
            responseData.put("total", result.getTotal());
            responseData.put("pages", result.getPages());
            responseData.put("current", result.getCurrent());
            responseData.put("size", result.getSize());
            
            log.info("获取书籍列表成功，页码: {}, 每页: {}, 总数: {}", page, size, result.getTotal());
            return R.ok(responseData);
        } catch (Exception e) {
            log.error("获取书籍列表失败", e);
            return R.error("获取书籍列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取书籍详情
     */
    @GetMapping("/{id}")
    public R<books> getBookById(@PathVariable Long id) {
        try {
            books book = booksService.getBook(id);
            if (book == null) {
                return R.error("书籍不存在");
            }
            return R.ok(book);
        } catch (Exception e) {
            log.error("获取书籍详情失败", e);
            return R.error("获取书籍详情失败: " + e.getMessage());
        }
    }

    /**
     * 添加书籍
     */
    @PostMapping("/add")
    public R<String> addBook(@RequestBody books book) {
        try {
            // 参数校验
            if (book.getBookName() == null || book.getBookName().trim().isEmpty()) {
                return R.error("书籍名称不能为空");
            }
            if (book.getCategoryId() == null) {
                return R.error("分类不能为空");
            }
            if (book.getPrice() == null || book.getPrice() <= 0) {
                return R.error("原价必须大于0");
            }
            if (book.getDiscountPrice() != null && book.getDiscountPrice() <= 0) {
                return R.error("折扣价必须大于0");
            }
            if (book.getDiscountPrice() != null && book.getDiscountPrice() >= book.getPrice()) {
                return R.error("折扣价必须低于原价");
            }
            if (book.getStock() == null || book.getStock() < 0) {
                return R.error("库存不能为负数");
            }

            // 校验分类是否存在
            categories category = categoriesService.getById(book.getCategoryId());
            if (category == null) {
                return R.error("选择的分类不存在");
            }
            book.setCategoryName(category.getName());

            // 设置创建时间
            book.setCreatedat(new java.util.Date());

            boolean success = booksService.save(book);
            if (success) {
                String priceInfo = book.getDiscountPrice() != null ? 
                    String.format("原价: %d分, 折扣价: %d分", book.getPrice(), book.getDiscountPrice()) : 
                    String.format("价格: %d分", book.getPrice());
                
                log.info("添加书籍成功: {}，作者: {}，{}", 
                        book.getBookName(), book.getAuthor(), priceInfo);
                return R.ok("添加书籍成功");
            } else {
                return R.error("添加书籍失败");
            }
        } catch (Exception e) {
            log.error("添加书籍失败", e);
            return R.error("添加书籍失败: " + e.getMessage());
        }
    }

    /**
     * 更新书籍信息
     */
    @PostMapping("/update")
    public R<String> updateBook(@RequestBody books book) {
        try {
            if (book.getId() == null) {
                return R.error("书籍ID不能为空");
            }

            books existingBook = booksService.getBook(book.getId());
            if (existingBook == null) {
                return R.error("书籍不存在");
            }

            // 参数校验
            if (book.getBookName() == null || book.getBookName().trim().isEmpty()) {
                return R.error("书籍名称不能为空");
            }
            if (book.getCategoryId() == null) {
                return R.error("分类不能为空");
            }
            if (book.getPrice() == null || book.getPrice() <= 0) {
                return R.error("原价必须大于0");
            }
            if (book.getDiscountPrice() != null && book.getDiscountPrice() <= 0) {
                return R.error("折扣价必须大于0");
            }
            if (book.getDiscountPrice() != null && book.getDiscountPrice() >= book.getPrice()) {
                return R.error("折扣价必须低于原价");
            }
            if (book.getStock() == null || book.getStock() < 0) {
                return R.error("库存不能为负数");
            }

            // 校验分类是否存在
            categories category = categoriesService.getById(book.getCategoryId());
            if (category == null) {
                return R.error("选择的分类不存在");
            }
            book.setCategoryName(category.getName());

            // 保留原有的创建时间
            book.setCreatedat(existingBook.getCreatedat());

            boolean success = booksService.updateById(book);
            if (success) {
                String priceInfo = book.getDiscountPrice() != null ? 
                    String.format("原价: %d分, 折扣价: %d分", book.getPrice(), book.getDiscountPrice()) : 
                    String.format("价格: %d分", book.getPrice());
                
                log.info("更新书籍成功: {}，作者: {}，{}", 
                        book.getBookName(), book.getAuthor(), priceInfo);
                return R.ok("更新书籍成功");
            } else {
                return R.error("更新书籍失败");
            }
        } catch (Exception e) {
            log.error("更新书籍失败", e);
            return R.error("更新书籍失败: " + e.getMessage());
        }
    }

    /**
     * 删除书籍
     */
    @PostMapping("/delete/{id}")
    public R<String> deleteBook(@PathVariable Long id) {
        try {
            books book = booksService.getBook(id);
            if (book == null) {
                return R.error("书籍不存在");
            }

            boolean success = booksService.removeById(id);
            if (success) {
                log.info("删除书籍成功: {}", book.getBookName());
                return R.ok("删除书籍成功");
            } else {
                return R.error("删除书籍失败");
            }
        } catch (Exception e) {
            log.error("删除书籍失败", e);
            return R.error("删除书籍失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除书籍
     */
    @PostMapping("/batchDelete")
    public R<String> batchDeleteBooks(@RequestBody List<Long> bookIds) {
        try {
            if (bookIds == null || bookIds.isEmpty()) {
                return R.error("请选择要删除的书籍");
            }

            boolean success = booksService.removeByIds(bookIds);
            if (success) {
                log.info("批量删除书籍成功，删除数量: {}", bookIds.size());
                return R.ok("批量删除成功");
            } else {
                return R.error("批量删除失败");
            }
        } catch (Exception e) {
            log.error("批量删除书籍失败", e);
            return R.error("批量删除失败: " + e.getMessage());
        }
    }

    /**
     * 更新书籍价格
     */
    @PostMapping("/updatePrice")
    public R<String> updateBookPrice(@RequestParam Long bookId, 
                                    @RequestParam Integer newPrice, 
                                    @RequestParam(required = false) Integer newDiscountPrice) {
        try {
            if (newPrice <= 0) {
                return R.error("原价必须大于0");
            }
            
            if (newDiscountPrice != null) {
                if (newDiscountPrice <= 0) {
                    return R.error("折扣价必须大于0");
                }
                if (newDiscountPrice >= newPrice) {
                    return R.error("折扣价必须低于原价");
                }
            }

            books book = booksService.getBook(bookId);
            if (book == null) {
                return R.error("书籍不存在");
            }

            Integer oldPrice = book.getPrice();
            Integer oldDiscountPrice = book.getDiscountPrice();
            
            book.setPrice(newPrice);
            book.setDiscountPrice(newDiscountPrice); // 可以为null
            
            boolean success = booksService.updateById(book);
            if (success) {
                StringBuilder logMessage = new StringBuilder()
                    .append("更新书籍价格成功: ")
                    .append(book.getBookName())
                    .append(" 原价从 ")
                    .append(oldPrice / 100.0)
                    .append(" 更新为 ")
                    .append(newPrice / 100.0);
                
                if (oldDiscountPrice != null || newDiscountPrice != null) {
                    logMessage.append(", 折扣价从 ")
                        .append(oldDiscountPrice != null ? oldDiscountPrice / 100.0 : "无")
                        .append(" 更新为 ")
                        .append(newDiscountPrice != null ? newDiscountPrice / 100.0 : "无");
                }
                
                log.info(logMessage.toString());
                return R.ok("价格更新成功");
            } else {
                return R.error("价格更新失败");
            }
        } catch (Exception e) {
            log.error("更新书籍价格失败", e);
            return R.error("价格更新失败: " + e.getMessage());
        }
    }

    /**
     * 批量调整价格
     */
    @PostMapping("/batchUpdatePrice")
    public R<String> batchUpdatePrice(@RequestBody Map<String, Object> params) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> bookIds = (List<Long>) params.get("bookIds");
            String adjustType = (String) params.get("adjustType");
            Double adjustValue = Double.valueOf(params.get("adjustValue").toString());
            Boolean adjustDiscountPrice = Boolean.valueOf(params.getOrDefault("adjustDiscountPrice", false).toString());

            if (bookIds == null || bookIds.isEmpty()) {
                return R.error("请选择要调价的书籍");
            }

            int updatedCount = 0;
            for (Long bookId : bookIds) {
                books book = booksService.getBook(bookId);
                if (book != null) {
                    // 调整原价
                    Integer oldPrice = book.getPrice();
                    Integer newPrice;
                    
                    if ("percentage".equals(adjustType)) {
                        // 按百分比调整
                        newPrice = (int) Math.round(oldPrice * (1 + adjustValue / 100));
                    } else {
                        // 按固定金额调整（转换为分）
                        newPrice = oldPrice + (int) Math.round(adjustValue * 100);
                    }
                    
                    if (newPrice <= 0) {
                        continue; // 跳过价格不合法的书籍
                    }
                    
                    book.setPrice(newPrice);
                    
                    // 如果需要同时调整折扣价，且书籍有折扣价
                    if (adjustDiscountPrice && book.getDiscountPrice() != null) {
                        Integer oldDiscountPrice = book.getDiscountPrice();
                        Integer newDiscountPrice;
                        
                        if ("percentage".equals(adjustType)) {
                            // 按百分比调整
                            newDiscountPrice = (int) Math.round(oldDiscountPrice * (1 + adjustValue / 100));
                        } else {
                            // 按固定金额调整（转换为分）
                            newDiscountPrice = oldDiscountPrice + (int) Math.round(adjustValue * 100);
                        }
                        
                        // 确保折扣价合法（大于0且小于原价）
                        if (newDiscountPrice > 0 && newDiscountPrice < newPrice) {
                            book.setDiscountPrice(newDiscountPrice);
                        } else {
                            // 如果调整后折扣价不合法，则清空折扣价
                            book.setDiscountPrice(null);
                        }
                    }
                    
                    if (booksService.updateById(book)) {
                        updatedCount++;
                    }
                }
            }

            log.info("批量调价完成，成功调整 {} 本书籍价格", updatedCount);
            return R.ok("批量调价成功，调整了 " + updatedCount + " 本书籍");
        } catch (Exception e) {
            log.error("批量调价失败", e);
            return R.error("批量调价失败: " + e.getMessage());
        }
    }

    /**
     * 调整库存
     */
    @PostMapping("/adjustStock")
    public R<String> adjustStock(@RequestParam Long bookId, @RequestParam Integer adjustment) {
        try {
            books book = booksService.getBook(bookId);
            if (book == null) {
                return R.error("书籍不存在");
            }

            Integer oldStock = book.getStock();
            Integer newStock = oldStock + adjustment;
            
            if (newStock < 0) {
                return R.error("库存不能为负数");
            }

            book.setStock(newStock);
            boolean success = booksService.updateById(book);
            if (success) {
                log.info("调整库存成功: {} 从 {} 调整为 {}", 
                        book.getBookName(), oldStock, newStock);
                return R.ok("库存调整成功");
            } else {
                return R.error("库存调整失败");
            }
        } catch (Exception e) {
            log.error("调整库存失败", e);
            return R.error("库存调整失败: " + e.getMessage());
        }
    }

    /**
     * 获取库存预警书籍
     */
    @GetMapping("/lowStock")
    public R<List<books>> getLowStockBooks(@RequestParam(defaultValue = "10") Integer threshold) {
        try {
            // TODO: 实现库存预警查询
            List<books> allBooks = booksService.list();
            List<books> lowStockBooks = allBooks.stream()
                    .filter(book -> book.getStock() <= threshold)
                    .toList();
            
            log.info("获取库存预警书籍，阈值: {}，预警数量: {}", threshold, lowStockBooks.size());
            return R.ok(lowStockBooks);
        } catch (Exception e) {
            log.error("获取库存预警书籍失败", e);
            return R.error("获取库存预警失败: " + e.getMessage());
        }
    }

    /**
     * 搜索书籍
     */
    @GetMapping("/search")
    public R<List<books>> searchBooks(@RequestParam String keyword) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return R.error("搜索关键词不能为空");
            }

            String searchKeyword = keyword.trim();

            // 使用 MyBatis-Plus 的查询条件
            LambdaQueryWrapper<books> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.like(books::getBookName, searchKeyword)
                    .or()
                    .like(books::getAuthor, searchKeyword);

            List<books> searchResults = booksService.list(queryWrapper);

            log.info("搜索书籍: {}，结果数量: {}", searchKeyword, searchResults.size());
            return R.ok(searchResults);
        } catch (Exception e) {
            log.error("搜索书籍失败", e);
            return R.error("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 根据分类ID获取书籍列表
     */
    @GetMapping("/category/{categoryId}")
    public R<List<books>> getBooksByCategory(@PathVariable Long categoryId) {
        try {
            // TODO: 实现根据分类查询书籍
            List<books> allBooks = booksService.list();
            List<books> categoryBooks = allBooks.stream()
                    .filter(book -> categoryId.equals(book.getCategoryId()))
                    .toList();
            
            log.info("获取分类 {} 下的书籍，数量: {}", categoryId, categoryBooks.size());
            return R.ok(categoryBooks);
        } catch (Exception e) {
            log.error("根据分类获取书籍失败", e);
            return R.error("获取分类书籍失败: " + e.getMessage());
        }
    }

    /**
     * 获取书籍统计信息
     */
    @GetMapping("/statistics")
    public R<Map<String, Object>> getBookStatistics() {
        try {
            List<books> allBooks = booksService.list();

            Map<String, Object> statistics = new java.util.HashMap<>();
            statistics.put("totalBooks", allBooks.size());
            statistics.put("totalStock", allBooks.stream().mapToInt(books::getStock).sum());
            statistics.put("lowStockCount", allBooks.stream()
                    .filter(book -> book.getStock() <= 10)
                    .count());
            statistics.put("averagePrice", allBooks.stream()
                    .mapToInt(books::getPrice)
                    .average()
                    .orElse(0.0) / 100.0); // 转换为元

            log.info("获取书籍统计信息成功");
            return R.ok(statistics);
        } catch (Exception e) {
            log.error("获取书籍统计信息失败", e);
            return R.error("获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 置顶/取消置顶书籍
     */
    @PostMapping("/togglePin")
    public R<String> togglePinBook(@RequestParam Long bookId) {
        try {
            books book = booksService.getBook(bookId);
            if (book == null) {
                return R.error("书籍不存在");
            }

            // 切换置顶状态
            Integer currentPinStatus = book.getIsPinned();
            Integer newPinStatus = (currentPinStatus != null && currentPinStatus == 1) ? 0 : 1;
            book.setIsPinned(newPinStatus);

            boolean success = booksService.updateById(book);
            if (success) {
                String action = newPinStatus == 1 ? "置顶" : "取消置顶";
                log.info("{} 书籍成功: {}", action, book.getBookName());
                return R.ok(action + "成功");
            } else {
                return R.error("操作失败");
            }
        } catch (Exception e) {
            log.error("置顶操作失败", e);
            return R.error("操作失败: " + e.getMessage());
        }
    }
} 