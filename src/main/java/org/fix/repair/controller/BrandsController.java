package org.fix.repair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.fix.repair.common.R;
import org.fix.repair.entity.brands;
import org.fix.repair.entity.categories;
import org.fix.repair.mapper.BrandsMapper;
import org.fix.repair.service.BrandsService;
import org.fix.repair.service.CategoriesService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/brand")
public class BrandsController {

    private final BrandsService brandsService;
    private final BrandsMapper<O> brandsMapper;
    private final CategoriesService categoriesService;

    public BrandsController(BrandsService brandsService, BrandsMapper<O> brandsMapper, CategoriesService categoriesService) {
        this.brandsService = brandsService;
        this.brandsMapper = brandsMapper;
        this.categoriesService = categoriesService;
    }

    // 品牌管理页面
    @GetMapping("/manage")
    public String managePage(Model model) {
        List<brands> brandsList = brandsService.list();
        List<categories> categoriesList = categoriesService.list();
        model.addAttribute("brands", brandsList);
        model.addAttribute("categories", categoriesList);
        return "brand/manage";
    }

    // API接口 - 添加品牌
    @PostMapping("/add")
    @ResponseBody
    public R<String> addBrand(@RequestBody brands brands) {
        brands.setCreatedat(new java.util.Date());
        return brandsService.save(brands) ? R.ok("添加成功") : R.error("添加失败");
    }

    // API接口 - 获取品牌列表
    @GetMapping("/list")
    @ResponseBody
    public R<List<brands>> listBrand(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                     @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                     @RequestParam(value = "categoryId", required = false) Long servicetypeid) {
        // 创建分页对象
        Page<brands> page = new Page<>(pageNum, pageSize);

        // 构建查询条件
        LambdaQueryWrapper<brands> queryWrapper = new LambdaQueryWrapper<>();

        if (servicetypeid != null) {
            queryWrapper.eq(brands::getCategoryId, servicetypeid);
        }

        // 添加排序（可选）
        queryWrapper.orderByDesc(brands::getCreatedat);
        List<brands> list = brandsMapper.selectPage(page, queryWrapper).getRecords();
        return R.ok(list);
    }

    // API接口 - 删除品牌
    @PostMapping("/delete")
    @ResponseBody
    public R<String> deleteBrand(@RequestParam Long id) {
        return brandsService.removeById(id) ? R.ok("删除成功") : R.error("删除失败");
    }
}
