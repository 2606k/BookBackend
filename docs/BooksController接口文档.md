# BooksController 接口文档

## 概述
BooksController 提供书籍管理的相关接口，包括书籍的增删改查、价格调整、库存管理等功能。

## 基础信息
- **基础路径**: `/api/books`
- **返回格式**: 统一使用 `R<T>` 格式
- **价格单位**: 价格以分为单位存储，前端显示时需要除以100转换为元

## 统一返回格式

### R<T> 响应格式
```json
{
  "code": 200,        // 状态码：200-成功，500-失败
  "msg": "操作成功",   // 响应消息
  "data": {}          // 响应数据
}
```

## 书籍实体结构

### books 实体字段
```json
{
  "id": 1,                    // 图书ID（自增主键）
  "categoryId": 1,           // 分类ID
  "bookName": "Java编程思想",  // 图书名称
  "author": "Bruce Eckel",    // 作者
  "publisher": "机械工业出版社", // 出版社
  "publishDate": "2020-01-01", // 出版日期
  "imageurl": "http://...",   // 图书图片URL
  "price": 9900,              // 图书价格（分）
  "description": "经典Java教程", // 图书描述
  "stock": 100,               // 图书库存
  "createdat": "2023-01-01T00:00:00" // 创建时间
}
```

## 接口列表

### 1. 获取书籍列表（分页查询）

**接口地址**: `GET /api/books/list`

**功能描述**: 获取书籍列表，支持分页、搜索、筛选等功能

**请求参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页数量 |
| bookName | String | 否 | - | 书籍名称（模糊查询） |
| minPrice | Integer | 否 | - | 最低价格（元） |
| maxPrice | Integer | 否 | - | 最高价格（元） |
| stockStatus | String | 否 | - | 库存状态：inStock-有库存，lowStock-低库存，outOfStock-无库存 |
| categoryId | Long | 否 | - | 分类ID |

**请求示例**:
```
GET /api/books/list?page=1&size=10&bookName=Java&minPrice=50&maxPrice=200&stockStatus=inStock&categoryId=1
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "categoryId": 1,
        "bookName": "Java编程思想",
        "author": "Bruce Eckel",
        "publisher": "机械工业出版社",
        "publishDate": "2020-01-01",
        "imageurl": "http://example.com/book1.jpg",
        "price": 9900,
        "description": "经典Java教程",
        "stock": 100,
        "createdat": "2023-01-01T00:00:00"
      }
    ],
    "total": 50,
    "pages": 5,
    "current": 1,
    "size": 10
  }
}
```

### 2. 根据ID获取书籍详情

**接口地址**: `GET /api/books/{id}`

**功能描述**: 根据书籍ID获取书籍详细信息

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 书籍ID |

**请求示例**:
```
GET /api/books/1
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "categoryId": 1,
    "bookName": "Java编程思想",
    "author": "Bruce Eckel",
    "publisher": "机械工业出版社",
    "publishDate": "2020-01-01",
    "imageurl": "http://example.com/book1.jpg",
    "price": 9900,
    "description": "经典Java教程",
    "stock": 100,
    "createdat": "2023-01-01T00:00:00"
  }
}
```

### 3. 添加书籍

**接口地址**: `POST /api/books/add`

**功能描述**: 添加新书籍

**请求体参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| bookName | String | 是 | 书籍名称 |
| categoryId | Long | 是 | 分类ID |
| author | String | 否 | 作者 |
| publisher | String | 否 | 出版社 |
| publishDate | String | 否 | 出版日期 |
| imageurl | String | 否 | 图片URL |
| price | Integer | 是 | 价格（分） |
| description | String | 否 | 描述 |
| stock | Integer | 是 | 库存 |

**请求示例**:
```json
{
  "bookName": "Spring Boot实战",
  "categoryId": 1,
  "author": "汪云飞",
  "publisher": "电子工业出版社",
  "publishDate": "2023-01-01",
  "imageurl": "http://example.com/spring-boot.jpg",
  "price": 8900,
  "description": "Spring Boot开发指南",
  "stock": 50
}
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": "添加书籍成功"
}
```

### 4. 更新书籍信息

**接口地址**: `POST /api/books/update`

**功能描述**: 更新书籍信息

**请求体参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 书籍ID |
| bookName | String | 是 | 书籍名称 |
| categoryId | Long | 是 | 分类ID |
| author | String | 否 | 作者 |
| publisher | String | 否 | 出版社 |
| publishDate | String | 否 | 出版日期 |
| imageurl | String | 否 | 图片URL |
| price | Integer | 是 | 价格（分） |
| description | String | 否 | 描述 |
| stock | Integer | 是 | 库存 |

**请求示例**:
```json
{
  "id": 1,
  "bookName": "Java编程思想（第4版）",
  "categoryId": 1,
  "author": "Bruce Eckel",
  "publisher": "机械工业出版社",
  "publishDate": "2020-01-01",
  "imageurl": "http://example.com/book1.jpg",
  "price": 10900,
  "description": "经典Java教程",
  "stock": 80
}
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": "更新书籍成功"
}
```

### 5. 删除书籍

**接口地址**: `POST /api/books/delete/{id}`

**功能描述**: 删除指定书籍

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 书籍ID |

**请求示例**:
```
POST /api/books/delete/1
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": "删除书籍成功"
}
```

### 6. 批量删除书籍

**接口地址**: `POST /api/books/batchDelete`

**功能描述**: 批量删除书籍

**请求体参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| - | List<Long> | 是 | 书籍ID列表 |

**请求示例**:
```json
[1, 2, 3, 4, 5]
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": "批量删除成功"
}
```

### 7. 更新书籍价格

**接口地址**: `POST /api/books/updatePrice`

**功能描述**: 更新指定书籍的价格

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| bookId | Long | 是 | 书籍ID |
| newPrice | Integer | 是 | 新价格（分） |

**请求示例**:
```
POST /api/books/updatePrice?bookId=1&newPrice=9900
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": "价格更新成功"
}
```

### 8. 批量调整价格

**接口地址**: `POST /api/books/batchUpdatePrice`

**功能描述**: 批量调整书籍价格

**请求体参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| bookIds | List<Long> | 是 | 书籍ID列表 |
| adjustType | String | 是 | 调整类型：percentage-按百分比，fixed-按固定金额 |
| adjustValue | Double | 是 | 调整值 |

**请求示例**:
```json
{
  "bookIds": [1, 2, 3],
  "adjustType": "percentage",
  "adjustValue": 10.0
}
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": "批量调价成功，调整了 3 本书籍"
}
```

### 9. 调整库存

**接口地址**: `POST /api/books/adjustStock`

**功能描述**: 调整书籍库存

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| bookId | Long | 是 | 书籍ID |
| adjustment | Integer | 是 | 调整数量（正数增加，负数减少） |

**请求示例**:
```
POST /api/books/adjustStock?bookId=1&adjustment=10
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": "库存调整成功"
}
```

### 10. 获取库存预警书籍

**接口地址**: `GET /api/books/lowStock`

**功能描述**: 获取库存低于阈值的书籍

**请求参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| threshold | Integer | 否 | 10 | 库存阈值 |

**请求示例**:
```
GET /api/books/lowStock?threshold=5
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": 1,
      "bookName": "Java编程思想",
      "stock": 3,
      "price": 9900
    }
  ]
}
```

### 11. 搜索书籍

**接口地址**: `GET /api/books/search`

**功能描述**: 根据关键词搜索书籍

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| keyword | String | 是 | 搜索关键词 |

**请求示例**:
```
GET /api/books/search?keyword=Java
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": 1,
      "bookName": "Java编程思想",
      "author": "Bruce Eckel",
      "price": 9900,
      "stock": 100
    }
  ]
}
```

### 12. 根据分类获取书籍列表

**接口地址**: `GET /api/books/category/{categoryId}`

**功能描述**: 根据分类ID获取该分类下的所有书籍

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| categoryId | Long | 是 | 分类ID |

**请求示例**:
```
GET /api/books/category/1
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": 1,
      "bookName": "Java编程思想",
      "categoryId": 1,
      "price": 9900,
      "stock": 100
    }
  ]
}
```

### 13. 获取书籍统计信息

**接口地址**: `GET /api/books/statistics`

**功能描述**: 获取书籍相关的统计信息

**请求示例**:
```
GET /api/books/statistics
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "totalBooks": 100,      // 总书籍数量
    "totalStock": 5000,     // 总库存数量
    "lowStockCount": 5,     // 低库存书籍数量
    "averagePrice": 99.5    // 平均价格（元）
  }
}
```

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 500 | 操作失败 |

## 注意事项

1. **价格单位**: 后端存储价格以分为单位，前端显示时需要除以100转换为元
2. **时间格式**: 时间字段使用ISO 8601格式
3. **分页查询**: 分页查询默认按创建时间倒序排列
4. **库存状态**: 
   - `inStock`: 库存大于0
   - `lowStock`: 库存小于等于10且大于0
   - `outOfStock`: 库存等于0
5. **批量操作**: 批量删除和批量调价操作建议限制单次操作数量，避免性能问题
6. **参数校验**: 所有必填参数都会进行校验，缺失或格式错误会返回相应错误信息

## 前端集成建议

1. **价格显示**: 前端显示价格时使用 `price / 100` 转换为元
2. **分页组件**: 建议使用分页组件处理书籍列表的分页显示
3. **搜索功能**: 可以实现实时搜索，使用防抖优化用户体验
4. **库存预警**: 可以定期调用库存预警接口，提醒管理员补货
5. **批量操作**: 提供批量选择功能，支持批量删除和调价操作
6. **图片处理**: 图片URL为空时显示默认图片
7. **错误处理**: 统一处理接口返回的错误信息，给用户友好的提示
