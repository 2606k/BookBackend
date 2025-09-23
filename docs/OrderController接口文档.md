# OrderController 接口文档

## 概述
OrderController是图书订单管理控制器，负责处理订单创建、支付、查询、退款等功能。

**基础路径**: `/appoint`

## 接口列表

### 1. 创建订单并发起支付
- **接口**: `POST /appoint/create`
- **功能**: 创建新订单并发起微信支付
- **请求参数**:
```json
{
  "openid": "用户openid",
  "name": "收件人姓名",
  "phone": "手机号码（11位）",
  "address": "收货地址",
  "remark": "订单备注（可选）",
  "bookItems": [
    {
      "bookId": "书籍ID",
      "quantity": "购买数量"
    }
  ]
}
```
- **响应结果**:
```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "appId": "微信小程序AppId",
    "timeStamp": "时间戳",
    "nonceStr": "随机字符串",
    "package": "预支付交易会话标识",
    "signType": "签名类型",
    "paySign": "签名"
  }
}
```

### 2. 查询订单
- **接口**: `GET /appoint/queryOrder`
- **功能**: 查询微信支付订单状态
- **请求参数**:
  - `outTradeNo`: 商户订单号
- **响应结果**:
```json
{
  "code": 200,
  "msg": "订单查询功能待实现",
  "data": null
}
```

### 3. 关闭订单
- **接口**: `GET /appoint/closeOrder/{outTradeNo}`
- **功能**: 关闭未支付订单
- **请求参数**:
  - `outTradeNo`: 商户订单号（路径参数）
- **响应结果**:
```json
{
  "code": 200,
  "msg": "订单关闭成功",
  "data": null
}
```

### 4. 微信支付回调
- **接口**: `POST /appoint/notify`
- **功能**: 处理微信支付成功回调
- **请求参数**: 微信支付回调数据（JSON格式）
- **响应结果**: `{"code":"SUCCESS","message":"成功"}`

### 5. 申请退款（用户端）
- **接口**: `POST /appoint/refund/apply`
- **功能**: 用户申请订单退款
- **请求参数**:
  - `orderId`: 订单ID
  - `reason`: 退款原因（可选）
- **响应结果**:
```json
{
  "code": 200,
  "msg": "申请退款成功，等待审核",
  "data": null
}
```

### 6. 执行退款（管理员端）
- **接口**: `POST /appoint/refund/execute`
- **功能**: 管理员执行订单退款
- **请求参数**:
  - `orderId`: 订单ID
  - `reason`: 退款原因（可选）
- **响应结果**:
```json
{
  "code": 200,
  "msg": "退款请求已发起，退款单号: xxx",
  "data": null
}
```

### 7. 退款回调
- **接口**: `POST /appoint/refund/notify`
- **功能**: 处理微信退款结果回调
- **请求参数**: 微信退款回调数据（JSON格式）
- **响应结果**: `{"code":"SUCCESS","message":"成功"}`

### 8. 订单列表查询
- **接口**: `GET /appoint/list`
- **功能**: 查询订单列表（支持用户端和管理端）
- **请求参数**:
  - `openid`: 用户openid（可选，为空时查询全部订单）
  - `address`: 地址关键词（可选，模糊查询）
  - `phone`: 手机号（可选，精确查询）
  - `name`: 姓名关键词（可选，模糊查询）
  - `status`: 订单状态（可选）
  - `page`: 当前页码（默认1）
  - `size`: 每页大小（默认10）
- **响应结果**:
```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "records": [
      {
        "id": "订单ID",
        "openid": "用户openid",
        "name": "收件人姓名",
        "phone": "手机号",
        "address": "收货地址",
        "num": "商品总数量",
        "money": "订单总金额",
        "status": "订单状态",
        "outTradeNo": "商户订单号",
        "remark": "订单备注",
        "createdat": "创建时间",
        "orderItems": [
          {
            "bookId": "书籍ID",
            "bookName": "书籍名称",
            "quantity": "购买数量",
            "price": "单价"
          }
        ]
      }
    ],
    "total": "总记录数",
    "current": "当前页",
    "size": "每页大小"
  }
}
```

## 订单状态说明
- `待支付`: 订单已创建，等待支付
- `0`: 已支付
- `1`: 申请退款
- `2`: 已退款

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
1. 所有金额单位为分
2. 手机号格式：11位数字，以1开头
3. 订单号格式：`order_` + UUID（无横线）
4. 退款单号格式：`refund_` + UUID（无横线）
5. 支付回调和退款回调需要在生产环境中增加签名验证和数据解密
6. 库存扣减在支付成功后进行，退款成功后恢复库存
