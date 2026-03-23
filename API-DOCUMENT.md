# LMS 催收管理系统 API 接口文档

## 基础信息

- **基础路径**: `/api` (需根据前端配置调整)
- **响应格式**: JSON
- **统一响应结构**:

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

---

## 1. 催收账户管理

### 1.1 获取催收账户列表

- **接口**: `POST /collection/account/list`
- **描述**: 分页查询催收账户列表

**请求参数**:
```json
{
  "customerId": "客户ID（可选）",
  "loanAccount": "贷款账户（可选）",
  "productCode": "产品代码（可选）",
  "overdueDays": 逾期天数（可选，整数）,
  "status": "账户状态（可选）",
  "page": {
    "currentPage": 1,
    "pageSize": 10
  }
}
```

**响应数据**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "list": [
      {
        "loanAccount": "贷款账户号",
        "customerId": "客户ID",
        "customerName": "客户姓名",
        "productCode": "产品代码",
        "productName": "产品名称",
        "loanAmount": "贷款金额",
        "loanBalance": "贷款余额",
        "overduePrincipal": "逾期本金",
        "overdueInterest": "逾期利息",
        "overdueDays": 逾期天数,
        "overdueTimes": 逾期次数,
        "status": "账户状态",
        "statusText": "状态文本",
        "collectionStatus": "催收状态",
        "lastCollectionTime": "最近催收时间",
        "lastCollectionResult": "最近催收结果",
        "createdAt": "创建时间",
        "updatedAt": "更新时间"
      }
    ]
  }
}
```

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
  "code": 200,
  "message": "success",
  "data": {
    "loanAccount": "贷款账户号",
    "customerId": "客户ID",
    "customerName": "客户姓名",
    "idCard": "身份证号",
    "phone": "手机号",
    "productCode": "产品代码",
    "productName": "产品名称",
    "loanAmount": "贷款金额",
    "loanBalance": "贷款余额",
    "overduePrincipal": "逾期本金",
    "overdueInterest": "逾期利息",
    "overdueDays": 逾期天数,
    "overdueTimes": 逾期次数,
    "status": "账户状态",
    "statusText": "状态文本",
    "collectionStatus": "催收状态",
    "lastCollectionTime": "最近催收时间",
    "lastCollectionResult": "最近催收结果",
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
  "code": 200,
  "message": "success",
  "data": [
    {
      "recordId": "记录ID",
      "loanAccount": "贷款账户号",
      "customerId": "客户ID",
      "method": "催收方式编码",
      "methodText": "催收方式文本",
      "result": "催收结果编码",
      "resultText": "催收结果文本",
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
  "code": 200,
  "message": "success",
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
  "code": 200,
  "message": "success",
  "data": [
    {
      "litigationId": "诉讼ID",
      "loanAccount": "贷款账户号",
      "customerId": "客户ID",
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
      "courtName": "法院名称",
      "lawFirm": "律所名称",
      "createdAt": "创建时间",
      "updatedAt": "更新时间"
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
  "code": 200,
  "message": "success",
  "data": {
    "litigationId": "诉讼ID",
    "loanAccount": "贷款账户号",
    "customerId": "客户ID",
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
    "litigationPreservationPaidAt": "诉讼保全支付时间",
    "litigationPreservationWriteOffAt": "诉讼保全核销时间",
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
- **描述**: 新增或更新诉讼信息（litigationId为空时新增，否则更新）

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
  "litigationPreservationPaidAt": "诉讼保全支付时间",
  "litigationPreservationWriteOffAt": "诉讼保全核销时间",
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
  "code": 200,
  "message": "success",
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
  "code": 200,
  "message": "success",
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

**文件命名规则**:
- 格式：`材料名_时间戳_随机码.扩展名`
- 示例：`合同文件_1710956400000_1234.pdf`
- 时间戳：13位毫秒级时间戳
- 随机码：4位随机数字

**响应数据**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "url": "document/2026-03-20/文件名_1710956400000_1234.xlsx",
    "path": "D:\\upload\\document\\2026-03-20\\文件名_1710956400000_1234.xlsx"
  }
}
```

**请求示例**:
```javascript
const formData = new FormData()
formData.append('file', file)
formData.append('materialType', 'document')
formData.append('materialName', file.name)

const result = await uploadFileApi(formData)
console.log(result.data.url)  // 文件访问URL
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

**请求示例**:
```javascript
const blob = await downloadMaterialApi(recordId)
downloadBlob(blob, fileName)  // 下载文件
```

---

### 5.3 更新催收材料（重交/补交）

- **接口**: `POST /collection/record/update-material`
- **描述**: 更新已有催收记录的材料文件（重交或补交）
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
  "code": 200,
  "message": "success",
  "data": {
    "id": "R17740036935692841",
    "recordId": "R17740036935692841",
    "materialType": "document",
    "materialName": "新文件名.xlsx",
    "materialUrl": "document/2026-03-20/新文件名_1234567890.xlsx"
  }
}
```

**请求示例**:
```javascript
const formData = new FormData()
formData.append('recordId', recordId)
formData.append('materialType', 'document')
formData.append('materialName', file.name)
formData.append('file', file)

await updateCollectionMaterialApi(formData)
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
  "code": 200,
  "message": "success",
  "data": {
    "total": 50,
    "list": [
      {
        "noticeId": "通知ID",
        "title": "通知标题",
        "content": "通知内容",
        "type": "通知类型",
        "readStatus": 0,
        "createdAt": "创建时间"
      }
    ]
  }
}
```

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
  "code": 200,
  "message": "success",
  "data": null
}
```

---

## 接口对照表（前后端匹配）

| 功能 | 前端API | 后端接口 | 状态 |
|------|---------|----------|------|
| 获取账户列表 | POST /collection/account/list | POST /collection/account/list | ✅ 匹配 |
| 获取账户详情 | GET /collection/account/detail/{loanAccount} | GET /collection/account/detail/{loanAccount} | ✅ 匹配 |
| 获取催收记录 | GET /collection/record/list/{loanAccount} | GET /collection/record/list/{loanAccount} | ✅ 匹配 |
| 新增催收记录 | POST /collection/record/add | POST /collection/record/add | ✅ 匹配 |
| 上传文件 | POST /collection/material/upload | POST /collection/material/upload | ✅ 匹配 |
| 下载材料 | GET /collection/material/download/{recordId} | GET /collection/material/download/{recordId} | ✅ 匹配 |
| 更新材料 | POST /collection/record/update-material | POST /collection/record/update-material | ✅ 匹配 |
| 获取诉讼列表 | GET /collection/litigation/list/{loanAccount} | GET /collection/litigation/list/{loanAccount} | ✅ 匹配 |
| 获取诉讼详情 | GET /collection/litigation/detail/{litigationId} | GET /collection/litigation/detail/{litigationId} | ✅ 匹配 |
| 更新诉讼信息 | POST /collection/litigation/update | POST /collection/litigation/update | ✅ 匹配 |
| 发送短信 | POST /collection/sms/send | POST /collection/sms/send | ✅ 匹配 |
| 获取通知列表 | POST /notice/list | POST /notice/list | ✅ 匹配 |
| 标记通知已读 | POST /notice/mark-read | POST /notice/mark-read | ✅ 匹配 |

---

## 错误码说明

| 错误码 | 描述 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

*文档生成时间: 2026-03-20*
