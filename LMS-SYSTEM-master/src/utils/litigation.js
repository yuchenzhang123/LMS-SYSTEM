// 诉讼相关共享常量与工具（供 account-detail.vue 与 LitigationDialog.vue 复用）

// 诉讼状态选项：code / label / inLitigation（是否处于诉讼中，决定费用字段可见）
export const LITIGATION_STATUS_OPTIONS = [
  { code: '1.1', label: '未起诉', inLitigation: false },
  { code: '1.2', label: '未起诉（已正常还款）', inLitigation: false },
  { code: '1.3', label: '准备材料起诉', inLitigation: true },
  { code: '1.4', label: '提交律所', inLitigation: true },
  { code: '2.1', label: '已起诉待立案', inLitigation: true },
  { code: '2.2', label: '已立案待开庭', inLitigation: true },
  { code: '2.3', label: '已待判决', inLitigation: true },
  { code: '3.1', label: '已判决待申请执行', inLitigation: true },
  { code: '3.2', label: '已申请执行待执行立案', inLitigation: true },
  { code: '3.3', label: '已执行立案', inLitigation: true },
  { code: '3.3.1', label: '执行拍卖中', inLitigation: true },
  { code: '3.3.2', label: '申请恢复执行', inLitigation: true },
  { code: '3.4', label: '已拍卖成功，待法院扣划', inLitigation: true },
  { code: '3.5', label: '中止执行', inLitigation: true },
  { code: '3.6', label: '终结本次执行', inLitigation: true },
  { code: '3.7', label: '终结执行【注意2年内恢复执行，一般3个月内恢复执行】', inLitigation: false },
  { code: '3.8', label: '申请再次恢复执行', inLitigation: true },
  { code: '3.9', label: '调解结案', inLitigation: false },
  { code: '4.1', label: '起诉后调解正常还款', inLitigation: false },
  { code: '4.2', label: '调解后仍未正常还款，拟恢复执行', inLitigation: true },
  { code: '4.3', label: '已结清', inLitigation: false },
  { code: '4.4', label: '撤诉（借款人死亡）', inLitigation: false },
  { code: '4.5', label: '法院不予受理', inLitigation: false }
]

// 已保存信息展示字段元信息（key / label，用于视图模式的 el-descriptions）
export const ALL_FIELDS = [
  { key: 'submitToLawFirmDate', label: '提交律所时间' },
  { key: 'lawFirm', label: '律所名称' },
  { key: 'submitToCourtDate', label: '提交法院时间' },
  { key: 'courtName', label: '涉及法院' },
  { key: 'filingDate', label: '诉讼立案时间' },
  { key: 'filingCaseNo', label: '诉讼立案案号' },
  { key: 'isHearing', label: '是否开庭' },
  { key: 'hearingDate', label: '开庭时间' },
  { key: 'judgmentDate', label: '判决时间' },
  { key: 'executionApplyToCourtDate', label: '执行申请提交时间' },
  { key: 'executionFilingDate', label: '执行立案时间' },
  { key: 'executionCaseNo', label: '执行立案案号' },
  { key: 'auctionStatus', label: '拍卖状态' },
  { key: 'litigationFee', label: '诉讼费' },
  { key: 'preservationFee', label: '保全费' },
  { key: 'appraisalFee', label: '评估费' },
  { key: 'litigationPreservationPaidAt', label: '诉讼和保全支付时间' },
  { key: 'litigationPreservationWriteOffAt', label: '诉讼和保全销账时间' },
  { key: 'lawyerFee', label: '律师费' }
]

// 字符串类型字段（切换状态时回退清空为 ''）
export const STRING_FIELDS = [
  'submitToLawFirmDate', 'lawFirm', 'submitToCourtDate', 'courtName', 'filingCaseNo',
  'hearingDate', 'judgmentDate', 'executionApplyToCourtDate', 'executionFilingDate',
  'filingDate', 'executionCaseNo', 'auctionStatus', 'litigationPreservationPaidAt', 'litigationPreservationWriteOffAt'
]

// 布尔类型字段（切换状态时回退清空为 false）
export const BOOL_FIELDS = [
  'litigationFeePaidByCustomer', 'preservationFeePaidByCustomer', 'lawyerFeePaidByCustomer'
]

// 构建诉讼表单：row 为已保存数据（编辑/查看时传入），传空对象 {} 即得到空表单（等价原 resetLitigationForm）
export function createLitigationForm(row = {}) {
  return {
    litigationId: row.litigationId || '',
    statusCode: row.statusCode || '',
    submitToLawFirmDate: row.submitToLawFirmDate || '',
    submitToCourtDate: row.submitToCourtDate || '',
    filingDate: row.filingDate || '',
    filingCaseNo: row.filingCaseNo || '',
    isHearing: !!row.isHearing,
    hearingDate: row.hearingDate || '',
    judgmentDate: row.judgmentDate || '',
    executionApplyToCourtDate: row.executionApplyToCourtDate || '',
    executionFilingDate: row.executionFilingDate || '',
    executionCaseNo: row.executionCaseNo || '',
    auctionStatus: row.auctionStatus || '',
    litigationFee: row.litigationFee || '',
    litigationFeePaidByCustomer: !!row.litigationFeePaidByCustomer,
    preservationFee: row.preservationFee || '',
    preservationFeePaidByCustomer: !!row.preservationFeePaidByCustomer,
    appraisalFee: row.appraisalFee || '',
    litigationPreservationPaidAt: row.litigationPreservationPaidAt || '',
    litigationPreservationWriteOffAt: row.litigationPreservationWriteOffAt || '',
    lawyerFee: row.lawyerFee || '',
    lawyerFeePaidByCustomer: !!row.lawyerFeePaidByCustomer,
    courtName: row.courtName || '',
    lawFirm: row.lawFirm || '',
    remark: row.remark || ''
  }
}

// LitigationDialog 编辑表单数据驱动渲染配置
// type: date | text | switch | money | textarea；money 类型的 paidKey 控制"客户已支付"复选框
export const EDIT_FIELDS = [
  { key: 'submitToLawFirmDate', label: '提交律所时间', type: 'date', placeholder: '请选择提交律所时间' },
  { key: 'lawFirm', label: '律所名称', type: 'text', placeholder: '请输入律所名称' },
  { key: 'submitToCourtDate', label: '提交法院时间', type: 'date', placeholder: '请选择提交法院时间' },
  { key: 'courtName', label: '涉及法院', type: 'text', placeholder: '请输入涉及法院' },
  { key: 'filingDate', label: '诉讼立案时间', type: 'date', placeholder: '请选择诉讼立案时间' },
  { key: 'filingCaseNo', label: '诉讼立案案号', type: 'text', placeholder: '请输入诉讼立案案号' },
  { key: 'isHearing', label: '是否开庭', type: 'switch' },
  { key: 'hearingDate', label: '开庭时间', type: 'date', placeholder: '请选择开庭时间' },
  { key: 'judgmentDate', label: '判决时间', type: 'date', placeholder: '请选择判决时间' },
  { key: 'executionApplyToCourtDate', label: '执行申请提交时间', type: 'date', placeholder: '请选择执行申请提交时间' },
  { key: 'executionFilingDate', label: '执行立案时间', type: 'date', placeholder: '请选择执行立案时间' },
  { key: 'executionCaseNo', label: '执行立案案号', type: 'text', placeholder: '请输入执行立案案号' },
  { key: 'auctionStatus', label: '拍卖状态', type: 'text', placeholder: '例如：一拍、二拍、变卖流拍' },
  { key: 'litigationFee', label: '诉讼费', type: 'money', placeholder: '请输入诉讼费金额', paidKey: 'litigationFeePaidByCustomer' },
  { key: 'preservationFee', label: '保全费', type: 'money', placeholder: '请输入保全费金额', paidKey: 'preservationFeePaidByCustomer' },
  { key: 'appraisalFee', label: '评估费', type: 'money', placeholder: '请输入评估费金额' },
  { key: 'litigationPreservationPaidAt', label: '诉讼和保全支付时间', type: 'date', placeholder: '请选择诉讼和保全支付时间' },
  { key: 'litigationPreservationWriteOffAt', label: '诉讼和保全销账时间', type: 'date', placeholder: '请选择诉讼和保全销账时间' },
  { key: 'lawyerFee', label: '律师费', type: 'money', placeholder: '请输入律师费金额', paidKey: 'lawyerFeePaidByCustomer' },
  { key: 'remark', label: '进度备注', type: 'textarea', placeholder: '请输入诉讼进度备注' }
]
