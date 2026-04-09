import request from '@/utils/request'

// 根据机构号获取角色（manager/staff/unknown）
export function getRoleByOrgCodeApi(orgCode) {
  return request({
    url: `${process.env.VUE_APP_API_PREFIX}/org/role`,
    method: 'get',
    params: { orgCode },
    _needsToken: true
  })
}

// 获取管辖行下所有分支行列表
export function getBranchesByOrgCodeApi(orgCode) {
  return request({
    url: `${process.env.VUE_APP_API_PREFIX}/org/branches`,
    method: 'get',
    params: { orgCode },
    _needsToken: true
  })
}

// 获取所有管辖行列表
export function getJurisdictionsApi() {
  return request({
    url: `${process.env.VUE_APP_API_PREFIX}/org/jurisdictions`,
    method: 'get',
    _needsToken: true
  })
}

// 获取完整机构树（管辖行 + 分支行）
export function getOrgTreeApi() {
  return request({
    url: `${process.env.VUE_APP_API_PREFIX}/org/tree`,
    method: 'get',
    _needsToken: true
  })
}

// 新增管辖行
export function addJurisdictionApi(orgCode, orgName) {
  return request({
    url: `${process.env.VUE_APP_API_PREFIX}/org/jurisdiction`,
    method: 'post',
    data: { orgCode, orgName },
    _needsToken: true
  })
}

// 新增分支行
export function addBranchApi(branchCode, branchName, orgCode) {
  return request({
    url: `${process.env.VUE_APP_API_PREFIX}/org/branch`,
    method: 'post',
    data: { branchCode, branchName, orgCode },
    _needsToken: true
  })
}

// 删除管辖行（同时删除其下所有分支行）
export function deleteJurisdictionApi(orgCode) {
  return request({
    url: `${process.env.VUE_APP_API_PREFIX}/org/jurisdiction/${orgCode}`,
    method: 'delete',
    _needsToken: true
  })
}

// 删除分支行
export function deleteBranchApi(branchCode) {
  return request({
    url: `${process.env.VUE_APP_API_PREFIX}/org/branch/${branchCode}`,
    method: 'delete',
    _needsToken: true
  })
}

// 从GBase查询机构号对应名称（辅助提示）
export function lookupOrgInGbaseApi(orgCode) {
  return request({
    url: `${process.env.VUE_APP_API_PREFIX}/org/gbase-lookup`,
    method: 'get',
    params: { orgCode },
    _needsToken: true
  })
}
