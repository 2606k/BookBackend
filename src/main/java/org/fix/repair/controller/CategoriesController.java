package org.fix.repair.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fix.repair.common.R;
import org.fix.repair.entity.categories;
import org.fix.repair.mapper.CategoriesMapper;
import org.fix.repair.service.CategoriesService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/category")
@AllArgsConstructor
public class CategoriesController {

    private final CategoriesService categoriesService;
    private final CategoriesMapper categoriesMapper;

    // 分类管理页面
    @GetMapping("/manage")
    public String managePage(Model model) {
        List<categories> list = categoriesService.list();
        model.addAttribute("categories", list);
        return "category/manage";
    }

    // 管理端分类管理页面 - 与admin路由保持一致
    @GetMapping("/admin/manage")
    public String adminManagePage(Model model) {
        log.info("访问管理端分类管理页面");
        return "categories-manage";
    }

    // API接口 - 添加分类
    @PostMapping("/add")
    @ResponseBody
    public R<String> addCategory(@RequestBody categories category) {
        try {
            // 参数校验
            if (category.getName() == null || category.getName().trim().isEmpty()) {
                return R.error("分类名称不能为空");
            }

            category.setCreatedat(new java.util.Date());
            boolean save = categoriesService.save(category);
            if (save) {
                log.info("添加分类成功: {}", category.getName());
                return R.ok("添加分类成功");
            }
            return R.error("添加分类失败");
        } catch (Exception e) {
            log.error("添加分类失败", e);
            return R.error("添加分类失败: " + e.getMessage());
        }
    }

    // API接口 - 更新分类
    @PostMapping("/update")
    @ResponseBody
    public R<String> updateCategory(@RequestBody categories category) {
        try {
            // 参数校验
            if (category.getServicetypeid() == null) {
                return R.error("分类ID不能为空");
            }
            if (category.getName() == null || category.getName().trim().isEmpty()) {
                return R.error("分类名称不能为空");
            }

            // 检查分类是否存在
            categories existingCategory = categoriesService.getById(category.getServicetypeid());
            if (existingCategory == null) {
                return R.error("分类不存在");
            }

            boolean update = categoriesService.updateById(category);
            if (update) {
                log.info("更新分类成功: {}", category.getName());
                return R.ok("更新分类成功");
            }
            return R.error("更新分类失败");
        } catch (Exception e) {
            log.error("更新分类失败", e);
            return R.error("更新分类失败: " + e.getMessage());
        }
    }

    // API接口 - 获取分类列表
    @GetMapping("/list")
    @ResponseBody
    public R<List<categories>> listCategory() {
        try {
            List<categories> list = categoriesService.list();
            log.info("获取分类列表成功，共 {} 个分类", list.size());
            return R.ok(list);
        } catch (Exception e) {
            log.error("获取分类列表失败", e);
            return R.error("获取分类列表失败: " + e.getMessage());
        }
    }

    // API接口 - 根据ID获取分类详情
    @GetMapping("/{id}")
    @ResponseBody
    public R<categories> getCategoryById(@PathVariable Long id) {
        try {
            categories category = categoriesService.getById(id);
            if (category == null) {
                return R.error("分类不存在");
            }
            return R.ok(category);
        } catch (Exception e) {
            log.error("获取分类详情失败", e);
            return R.error("获取分类详情失败: " + e.getMessage());
        }
    }

    // API接口 - 删除分类
    @PostMapping("/delete/{id}")
    @ResponseBody
    public R<String> deleteCategory(@PathVariable Long id) {
        try {
            // 检查分类是否存在
            categories category = categoriesService.getById(id);
            if (category == null) {
                return R.error("分类不存在");
            }

            boolean remove = categoriesService.removeById(id);
            if (remove) {
                log.info("删除分类成功: {}", category.getName());
                return R.ok("删除分类成功");
            }
            return R.error("删除分类失败");
        } catch (Exception e) {
            log.error("删除分类失败", e);
            return R.error("删除分类失败: " + e.getMessage());
        }
    }

    // 兼容旧版本的删除接口
    @PostMapping("/delete")
    @ResponseBody
    public R<String> deleteCategoryOld(@RequestParam Long id) {
        return deleteCategory(id);
    }
}
