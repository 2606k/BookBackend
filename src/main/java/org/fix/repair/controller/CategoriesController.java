package org.fix.repair.controller;

import org.fix.repair.common.R;
import org.fix.repair.entity.categories;
import org.fix.repair.mapper.CategoriesMapper;
import org.fix.repair.service.CategoriesService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/category")
public class CategoriesController {

    private CategoriesService categoriesService;
    private CategoriesMapper categoriesMapper;
    public CategoriesController(CategoriesService categoriesService, CategoriesMapper categoriesMapper){
        this.categoriesService = categoriesService;
        this.categoriesMapper = categoriesMapper;
    }

    // 分类管理页面
    @GetMapping("/manage")
    public String managePage(Model model) {
        List<categories> list = categoriesService.list();
        model.addAttribute("categories", list);
        return "category/manage";
    }

    // API接口 - 添加分类
    @PostMapping("/add")
    @ResponseBody
    public R<String> addCategory(@RequestBody categories categories){
        categories.setCreatedat(new java.util.Date());
        boolean save = categoriesService.save(categories);
        if (save){
            return R.ok("添加成功");
        }
        return R.error("添加失败");
    }

    // API接口 - 获取分类列表
    @GetMapping("/list")
    @ResponseBody
    public R<List<categories>> listCategory(){
        List<categories> list = categoriesService.list();
        return R.ok(list);
    }

    // API接口 - 删除分类
    @PostMapping("/delete")
    @ResponseBody
    public R<String> deleteCategory(@RequestParam Long id){
        boolean remove = categoriesService.removeById(id);
        if (remove){
            return R.ok("删除成功");
        }
        return R.error("删除失败");
    }

}
