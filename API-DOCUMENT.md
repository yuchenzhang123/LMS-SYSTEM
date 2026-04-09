# LMS 催收管理系统 API 接口文档

## 基础信息

- **基础路径**: `/api` (需根据前端配置调整)
- **响应格式**: JSON
- **统一响应结构**:

```json
{
  "code": "0",
  "message": "成功",
  "data": { ... }
}
```

**字段说明**:
| 字段 | 类型 | 描述 |
|------|------|------|
| code | String | 状态码，"0" 表示成功，其他表示错误 |
| message | String | 提示信息，成功或错误描述 |
| data | Object/Array | 返回数据，成功时有值 |

---

## 错误响应格式

当请求失败时，返回格式如下：

```json
{
  "code": "1001",
  "message": "参数校验失败: 文件大小超过限制",
  "data": null
}
```

前端应统一从 `message` 字段获取错误信息并展示，**不应在前端硬编码错误信息**。

---

## 错误码说明

| 错误码 | 描述 |
|--------|------|
| 0 | 成功 |
| 400 | 参数错误 |
| 1001 | 参数错误 |
| 1002 | 权限不足 |
| 1003 | 未登录 |
| 1004 | 系统异常 |
| 1005 | 业务异常 |

---

## 1. 催收账户管理

### 1.1 获取催收账户列表

- **接口**: `POST /collection/account/list`
- **描述**: 分页查询催收账户列表，按 `branchCode`（分支行号）过滤

**请求参数**:
```json
{
  "customerId": "客户ID（可选）",
  "loanAccount": "贷款账户（可选）",
  "productCode": "产品代码（可选）",
  "overdueDays": 逾期天数（可选，整数）,
  "status": "账户状态（可选）",
  "branchCode": "分支行号（可选，对应 LOAN_BRANCH_NO）",
  "page": {
    "currentPage": 1,
    "pageSize": 10
  }
}
```

> 说明：
> - 业务员视角：前端传入自身机构号作为 `branchCode`，后端只返回该分支行的数据。
> - 管辖行/管理员视角：前端传入选中的业务机构 `branchCode`，后端返回对应分支行数据。
> - `branchCode` 为空时不做机构过滤，返回全量数据（仅用于不需要机构隔离的场景）。

**响应数据**:
```json
{
  "code": "0",
  "message": "成功",
  "data": {
    "total": 100,
    "records": [
      {
        "loanAccount": "贷款账户号",
        "customerId": "客户ID",
        "customerName": "客户姓名",
        "productCode": "产品代码",
        "productName": "产品名称",
        "loanBalance": "贷款余额",
        "overduePrincipal": "逾期本金",
        "overdueInterest": "逾期利息",
        "overdueDays": 逾期天数,
        "branchCode": "分支行号",
        "branchName": "分支行名称",
        "status": "账户状态",
        "statusUpdateTime": "状态最后更新时间"
      }
    ],
    "current": 1,
    "size": 10
  }
}
```

> 账户状态值：`uncollected`（未催收）/ `collecting`（催收中）/ `completed`（已完成）

---

### 1.2 获取账户详情

- **接口**: `GET /collection/account/detail/{loanAccount}`
- **描述**: 根据贷款账户号获取账户详细信息

**路径参数**:
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| loanAccount | String | 是 | 贷款账户号 |

**响应数据**:
```json
{
  "code": "0",
  "message": "成功",
  "data": {
    "loanAccount": "贷款账户号",
    "customerId": "客户ID",
    "customerName": "客户姓名",
    "phone": "手机号",
    "productCode": "产品代码",
    "productName": "产品名称",
    "loanBalance": "贷款余额",
    "overduePrincipal": "逾期本金",
    "overdueInterest": "逾期利息",
    "overdueDays": 逾期天数,
    "orgCode": "管辖行号",
    "branchCode": "分支行号",
    "branchName": "分支行名称",
    "status": "账户状态",
    "gbaseSyncTime": "GBase同步时间",
    "createdAt": "创建时间",
    "updatedAt": "更新时间"
  }
}
```

---

## 2. 催收记录管理

### 2.1 获取催收记录列表

- **接口**: `GET /collection/record/list/{loanAccount}`
- **描述**: 根据贷款账户号获取催收记录列表

**路径参数**:
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| loanAccount | String | 是 | 贷款账户号 |

**响应数据**:
```json
{
  "code": "0",
  "message": "成功",
  "data": [
    {
      "recordId": "记录ID",
      "loanAccount": "贷款账户号",
      "customerId": "客户ID",
      "method": "催收方式编码",
      "methodText": "催收方式文本",
      "result": "催收结果编码",
      "operatorId": "操作员ID",
      "operatorName": "操作员姓名",
      "time": "催收时间",
      "remark": "备注",
      "materialType": "材料类型",
      "materialName": "材料名称",
      "materialUrl": "材料URL",
      "createdAt": "创建时间"
    }
  ]
}
```

---

### 2.2 新增催收记录

- **接口**: `POST /collection/record/add`
- **描述**: 新增一条催收记录

**请求参数**:
```json
{
  "loanAccount": "贷款账户号（必填）",
  "customerId": "客户ID（必填）",
  "method": "催收方式编码（必填）",
  "methodText": "催收方式文本（必填）",
  "result": "催收结果编码（必填）",
  "operatorId": "操作员ID（可选）",
  "operatorName": "操作员姓名（可选）",
  "time": "催收时间（可选，格式：yyyy-MM-dd HH:mm:ss）",
  "remark": "备注（可选）",
  "materialType": "材料类型（可选）",
  "materialName": "材料名称（可选）",
  "materialUrl": "材料URL（可选）"
}
```

**响应数据**:
```json
{
  "code": "0",
  "message": "成功",
  "data": {
    "recordId": "新生成的记录ID"
  }
}
```

---

## 3. 诉讼信息管理

### 3.1 获取诉讼信息列表

- **接口**: `GET /collection/litigation/list/{loanAccount}`
- **描述**: 根据贷款账户号获取诉讼信息列表

**路径参数**:
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| loanAccount | String | 是 | 贷款账户号 |

**响应数据**:
```json
{
  "code": "0",
  "message": "成功",
  "data": [
    {
      "litigationId": "诉讼ID",
      "loanAccount": "贷款账户号",
      "statusCode": "诉讼状态编码",
      "statusText": "诉讼状态文本",
      "inLitigation": true,
      "submitToLawFirmDate": "提交律所日期",
      "submitToCourtDate": "提交法院日期",
      "filingCaseNo": "立案案号",
      "isHearing": false,
      "hearingDate": "开庭日期",
      "judgmentDate": "判决日期",
      "executionCaseNo": "执行案号",
      "courtName": "法院名称",
      "lawFirm": "律所名称",
      "updatedAt": "最近更新时间"
    }
  ]
}
```

---

### 3.2 获取诉讼详情

- **接口**: `GET /collection/litigation/detail/{litigationId}`
- **描述**: 根据诉讼ID获取诉讼详细信息

**路径参数**:
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| litigationId | String | 是 | 诉讼ID |

**响应数据**:
```json
{
  "code": "0",
  "message": "成功",
  "data": {
    "litigationId": "诉讼ID",
    "loanAccount": "贷款账户号",
    "statusCode": "诉讼状态编码",
    "statusText": "诉讼状态文本",
    "inLitigation": true,
    "submitToLawFirmDate": "提交律所日期",
    "submitToCourtDate": "提交法院日期",
    "filingCaseNo": "立案案号",
    "isHearing": false,
    "hearingDate": "开庭日期",
    "judgmentDate": "判决日期",
    "executionApplyToCourtDate": "申请执行日期",
    "executionFilingDate": "执行立案日期",
    "executionCaseNo": "执行案号",
    "auctionStatus": "拍卖状态",
    "litigationFee": "诉讼费",
    "litigationFeePaidByCustomer": false,
    "preservationFee": "保全费",
    "preservationFeePaidByCustomer": false,
    "appraisalFee": "评估费",
    "lawyerFee": "律师费",
    "lawyerFeePaidByCustomer": false,
    "courtName": "法院名称",
    "lawFirm": "律所名称",
    "remark": "备注",
    "operatorId": "操作员ID",
    "operatorName": "操作员姓名",
    "createdAt": "创建时间",
    "updatedAt": "更新时间"
  }
}
```

---

### 3.3 更新诉讼信息

- **接口**: `POST /collection/litigation/update`
- **描述**: 新增或更新诉讼信息（`litigationId` 为空时新增，否则更新）

**请求参数**:
```json
{
  "litigationId": "诉讼ID（更新时必填，新增时为空）",
  "loanAccount": "贷款账户号（必填）",
  "customerId": "客户ID（必填）",
  "statusCode": "诉讼状态编码（必填）",
  "statusText": "诉讼状态文本（必填）",
  "inLitigation": true,
  "submitToLawFirmDate": "提交律所日期",
  "submitToCourtDate": "提交法院日期",
  "filingCaseNo": "立案案号",
  "isHearing": false,
  "hearingDate": "开庭日期",
  "judgmentDate": "判决日期",
  "executionApplyToCourtDate": "申请执行日期",
  "executionFilingDate": "执行立案日期",
  "executionCaseNo": "执行案号",
  "auctionStatus": "拍卖状态",
  "litigationFee": "诉讼费",
  "litigationFeePaidByCustomer": false,
  "preservationFee": "保全费",
  "preservationFeePaidByCustomer": false,
  "appraisalFee": "评估费",
  "lawyerFee": "律师费",
  "lawyerFeePaidByCustomer": false,
  "courtName": "法院名称",
  "lawFirm": "律所名称",
  "remark": "备注",
  "operatorId": "操作员ID",
  "operatorName": "操作员姓名"
}
```

**响应数据**:
```json
{
  "code": "0",
  "message": "成功",
  "data": {
    "litigationId": "诉讼ID"
  }
}
```

---

## 4. 短信催收

### 4.1 发送催收短信

- **接口**: `POST /collection/sms/send`
- **描述**: 发送催收短信

**请求参数**:
```json
{
  "loanAccount": "贷款账户号（必填）",
  "customerId": "客户ID（必填）",
  "templateId": "短信模板ID（可选）",
  "content": "短信内容（必填）",
  "phone": "手机号（可选）"
}
```

**响应数据**:
```json
{
  "code": "0",
  "message": "成功",
  "data": null
}
```

---

## 5. 文件管理

### 5.1 上传文件

- **接口**: `POST /collection/material/upload`
- **描述**: 上传催收材料文件
- **Content-Type**: `multipart/form-data`

**请求参数**:
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| file | File | 是 | 文件对象 |
| materialType | String | 否 | 材料类型（document/image/video/audio/archive） |
| materialName | String | 否 | 材料名称 |

**文件类型限制**:
- 最大大小: 10MB
- 支持的文件类型:
  - **document**: pdf, doc, docx, txt, xlsx, xls, pptx, ppt, csv, rtf, zip, rar, 7z
  - **image**: jpg, jpeg, png, gif, pdf, bmp, webp
  - **video**: mp4, avi, mov, wmv, flv, mkv
  - **audio**: mp3, wav, aac
  - **archive**: zip, rar, 7z, tar, gz

**响应数据**:
```json
{
  "code": "0",
  "message": "成功",
  "data": {
    "url": "document/2026-04-09/文件名_1710956400000_1234.xlsx",
    "path": "/upload/document/2026-04-09/文件名_1710956400000_1234.xlsx"
  }
}
```

---

### 5.2 下载催收材料

- **接口**: `GET /collection/material/download/{recordId}`
- **描述**: 下载催收记录中的材料文件
- **响应类型**: `blob`

**路径参数**:
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| recordId | String | 是 | 催收记录ID |

**响应**: 文件流（blob）

---

### 5.3 更新催收材料（重交/补交）

- **接口**: `POST /collection/record/update-material`
- **描述**: 更新已有催收记录的材料文件
- **Content-Type**: `multipart/form-data`

**请求参数**:
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| recordId | String | 是 | 催收记录ID |
| materialType | String | 否 | 材料类型 |
| materialName | String | 否 | 材料名称 |
| file | File | 是 | 新的文件对象 |

**响应数据**:
```json
{
  "code": "0",
  "message": "成功",
  "data": {
    "recordId": "R17740036935692841",
    "materialType": "document",
    "materialName": "新文件名.xlsx",
    "materialUrl": "document/2026-04-09/新文件名_1234567890.xlsx"
  }
}
```

---

## 6. 通知管理

### 6.1 获取通知列表

- **接口**: `POST /notice/list`
- **描述**: 分页查询通知列表

**请求参数**:
```json
{
  "page": {
    "currentPage": 1,
    "pageSize": 10
  },
  "readStatus": 0
}
```

**参数说明**:
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| page.currentPage | Integer | 否 | 当前页码，默认1 |
| page.pageSize | Integer | 否 | 每页大小，默认10 |
| readStatus | Integer | 否 | 读取状态：0-未读，1-已读 |

**响应数据**:
```json
{
  "code": "0",
  "message": "成功",
  "data": {
    "total": 50,
    "list": [
      {
        "noticeId": "通知ID",
        "title": "通知标题",
        "content": "通知内容",
        "noticeType": "new_overdue 或 collecting_completed",
        "readStatus": 0,
        "createdAt": "创建时间"
      }
    ]
  }
}
```

> `noticeType` 常见值：`new_overdue`（新增逾期）、`collecting_completed`（催收完成还款）。后端自动避免 30 分钟内同账号重复同类型通知。

---

### 6.2 标记通知已读

- **接口**: `POST /notice/mark-read`
- **描述**: 将指定通知标记为已读

**请求参数**:
```json
{
  "noticeIds": ["通知ID1", "通知ID2"]
}
```

**响应数据**:
```json
{
  "code": "0",
  "message": "成功",
  "data": null
}
```

---

## 7. 机构层级管理

> 权限要求：`admin` 角色可执行所有操作；`manager`/`staff` 仅可使用查询接口。

### 7.1 获取角色（按机构号）

- **接口**: `GET /org/role`
- **描述**: 根据机构号判断用户角色

**请求参数**:
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| orgCode | String | 是 | 机构号（来自 SSO 的 ehrNo） |

**响应数据**:
```json
{
  "code": "0",
  "message": "成功",
  "data": "manager"
}
```

> 返回值：`admin`（系统管理员，机构号在配置文件 `org.admin.codes` 中）/ `manager`（管辖行管理员，在 `jurisdiction_org` 表中）/ `staff`（分支行业务员，在 `branch_org` 表中）/ `unknown`（无权限）

---

### 7.2 获取管辖行下的分支行列表

- **接口**: `GET /org/branches`
- **描述**: 获取指定管辖行下的所有分支行

**请求参数**:
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| orgCode | String | 是 | 管辖行机构号 |

**响应数据**:
```json
{
  "code": "0",
  "message": "成功",
  "data": [
    { "branchCode": "分支行号", "branchName": "分支行名称" }
  ]
}
```

---

### 7.3 获取所有管辖行列表

- **接口**: `GET /org/jurisdictions`
- **描述**: 获取所有管辖行

**响应数据**:
```json
{
  "code": "0",
  "message": "成功",
  "data": [
    { "orgCode": "管辖行号", "orgName": "管辖行名称" }
  ]
}
```

---

### 7.4 获取完整机构树

- **接口**: `GET /org/tree`
- **描述**: 返回管辖行及其下属分支行的完整树形结构

**响应数据**:
```json
{
  "code": "0",
  "message": "成功",
  "data": [
    {
      "orgCode": "管辖行号",
      "orgName": "管辖行名称",
      "type": "manager",
      "children": [
        {
          "branchCode": "分支行号",
          "branchName": "分支行名称",
          "orgCode": "所属管辖行号",
          "type": "staff"
        }
      ]
    }
  ]
}
```

---

### 7.5 新增管辖行

- **接口**: `POST /org/jurisdiction`
- **权限**: 仅 `admin`

**请求参数**:
```json
{
  "orgCode": "管辖行号（必填）",
  "orgName": "管辖行名称（必填）"
}
```

**响应数据**:
```json
{ "code": "0", "message": "新增管辖行成功", "data": null }
```

---

### 7.6 新增分支行

- **接口**: `POST /org/branch`
- **权限**: 仅 `admin`

**请求参数**:
```json
{
  "branchCode": "分支行号（必填）",
  "branchName": "分支行名称（必填）",
  "orgCode": "所属管辖行号（必填）"
}
```

**响应数据**:
```json
{ "code": "0", "message": "新增分支行成功", "data": null }
```

---

### 7.7 删除管辖行

- **接口**: `DELETE /org/jurisdiction/{orgCode}`
- **权限**: 仅 `admin`
- **描述**: 同时级联删除该管辖行下所有分支行

**路径参数**:
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| orgCode | String | 是 | 管辖行号 |

**响应数据**:
```json
{ "code": "0", "message": "删除管辖行成功", "data": null }
```

---

### 7.8 删除分支行

- **接口**: `DELETE /org/branch/{branchCode}`
- **权限**: 仅 `admin`

**路径参数**:
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| branchCode | String | 是 | 分支行号 |

**响应数据**:
```json
{ "code": "0", "message": "删除分支行成功", "data": null }
```

---

### 7.9 GBase 机构号查询（辅助提示）

- **接口**: `GET /org/gbase-lookup`
- **描述**: 在 GBase `rcrms.R_V_O_ORG_BASIC` 表中查询机构号对应的名称，用于新增机构时辅助填充名称或提示不存在

**请求参数**:
| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| orgCode | String | 是 | 待查询的机构号（`ORG_REFNO`） |

**响应数据**:
```json
{
  "code": "0",
  "message": "成功",
  "data": {
    "found": true,
    "orgName": "xx支行"
  }
}
```

> `found` 为 `false` 时表示 GBase 中不存在该机构号，前端给出提示但不阻止保存。

---

## 8. 定时任务手动触发

> 所有触发接口均为 `POST`，无请求体，成功返回 `{ "code": "0", "data": "..." }`。

### 8.1 手动触发 GBase 数据同步

- **接口**: `POST /admin/scheduler/sync-gbase`
- **描述**: 手动触发 GBase 贷款账户数据同步（等同于每日凌晨1点的定时任务）

---

### 8.2 手动触发催收中→已完成状态变更

- **接口**: `POST /admin/scheduler/collecting-to-completed`
- **描述**: 手动触发催收中账户的状态变更检查

---

## 外部接口依赖（SSO / 权限服务）

以下接口由外部服务提供，通过 nginx 代理转发，前端使用对应的环境变量前缀访问。

| 接口 | nginx 前缀 | 描述 |
|------|-----------|------|
| `POST /sso/tokenCheck` | `/ssoservice` | SSO Token 校验，返回 `userInfo`（含 `userId`、`orgId` 等） |
| `GET /oauth/token` | `/oauth` | 获取 OAuth2 client_credentials 访问令牌 |
| `GET /model/menulist/userId` | `/api/model` | 获取动态菜单列表（按 userId + sysId） |
| `GET /api/orginfo/orgid/{orgId}` | `/api/orginfo` | 根据 orgId 查询机构信息，返回含 `ehrNo`（机构号）的 JSON |

---

## 接口对照表

| 功能 | 方法 | 接口路径 | 备注 |
|------|------|----------|------|
| 获取账户列表 | POST | /collection/account/list | 支持 branchCode 过滤 |
| 获取账户详情 | GET | /collection/account/detail/{loanAccount} | |
| 获取催收记录 | GET | /collection/record/list/{loanAccount} | |
| 新增催收记录 | POST | /collection/record/add | |
| 上传文件 | POST | /collection/material/upload | |
| 下载材料 | GET | /collection/material/download/{recordId} | |
| 更新材料 | POST | /collection/record/update-material | |
| 获取诉讼列表 | GET | /collection/litigation/list/{loanAccount} | |
| 获取诉讼详情 | GET | /collection/litigation/detail/{litigationId} | |
| 更新诉讼信息 | POST | /collection/litigation/update | |
| 发送短信 | POST | /collection/sms/send | |
| 获取通知列表 | POST | /notice/list | |
| 标记通知已读 | POST | /notice/mark-read | |
| 获取角色 | GET | /org/role | |
| 获取分支行列表 | GET | /org/branches | |
| 获取管辖行列表 | GET | /org/jurisdictions | |
| 获取机构树 | GET | /org/tree | |
| 新增管辖行 | POST | /org/jurisdiction | 仅 admin |
| 新增分支行 | POST | /org/branch | 仅 admin |
| 删除管辖行 | DELETE | /org/jurisdiction/{orgCode} | 仅 admin，级联删分支行 |
| 删除分支行 | DELETE | /org/branch/{branchCode} | 仅 admin |
| GBase 机构查询 | GET | /org/gbase-lookup | 辅助提示 |
| 手动触发GBase同步 | POST | /admin/scheduler/sync-gbase | |
| 手动触发状态变更 | POST | /admin/scheduler/collecting-to-completed | |

---

*文档更新时间: 2026-04-09*
