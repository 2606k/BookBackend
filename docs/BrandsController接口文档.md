# BrandsController 接口文档

## 概述
BrandsController是书籍（品牌）管理控制器，负责处理书籍的增删改查功能。提供Web页面管理和API接口两种方式。

**基础路径**: `/brand`

## Web页面接口

### 1. 品牌管理页面
- **接口**: `GET /brand/manage`
- **功能**: 返回品牌管理页面
- **响应**: 返回 `brand/manage.html` 页面，包含品牌列表和分类列表数据

## API接口列表

### 1. 添加品牌（书籍）
- **接口**: `POST /brand/add`
- **功能**: 添加新的书籍
- **请求参数**:
```json
{
  "categoryId": "分类ID",
  "bookName": "书籍名称",
  "imageurl": "书籍图片URL",
  "price": "书籍价格（单位：分）",
  "description": "书籍描述",
  "stock": "库存数量"
}
```
- **响应结果**:
```json
{
  "code": 200,
  "msg": "添加成功",
  "data": null
}
```

### 2. 获取品牌（书籍）列表
- **接口**: `GET /brand/list`
- **功能**: 分页查询书籍列表
- **请求参数**:
  - `pageNum`: 当前页码（默认1）
  - `pageSize`: 每页大小（默认10）
  - `categoryId`: 分类ID（可选，用于筛选特定分类的书籍）
- **响应结果**:
```json
{
  "code": 200,
  "msg": "成功",
  "data": [
    {
      "id": "书籍ID",
      "categoryId": "分类ID",
      "bookName": "书籍名称",
      "imageurl": "书籍图片URL",
      "price": "书籍价格",
      "description": "书籍描述",
      "stock": "库存数量",
      "createdat": "创建时间"
    }
  ]
}
```

### 3. 删除品牌（书籍）
- **接口**: `POST /brand/delete`
- **功能**: 删除指定的书籍
- **请求参数**:
  - `id`: 书籍ID（查询参数）
- **响应结果**:
```json
{
  "code": 200,
  "msg": "删除成功",
  "data": null
}
```

## 分页说明
- **pageNum**: 页码从1开始
- **pageSize**: 支持自定义每页显示数量
- **排序**: 按创建时间降序排列，最新的书籍排在前面

## 筛选功能
- **按分类筛选**: 通过`categoryId`参数可以筛选特定分类的书籍
- **支持空筛选**: 不传`categoryId`时返回所有书籍

## 错误处理
所有接口在出现错误时返回统一格式：
```json
{
  "code": 500,
  "msg": "错误描述",
  "data": null
}
```

## 注意事项
1. 价格单位为分，前端显示时需要转换为元
2. 图片URL需要是完整的可访问地址
3. 库存为0时前端应做相应提示
4. 删除书籍前应检查是否有相关订单
5. 该Controller实际管理的是books实体，命名为Brand是历史原因
