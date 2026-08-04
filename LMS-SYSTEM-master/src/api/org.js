import request from '@/utils/request'
import { APP_CONFIG } from '@/config'

export function getRoleByOrgCodeApi(orgCode, ehrNo) {
  return request({
    url: `${APP_CONFIG.API_URL}/org/role`,
    method: 'get',
    params: ehrNo ? { orgCode, ehrNo } : { orgCode },
    _needsToken: true
  })
}

export function getBranchesByOrgCodeApi(orgCode) {
  return request({
    url: `${APP_CONFIG.API_URL}/org/branches`,
    method: 'get',
    params: { orgCode },
    _needsToken: true
  })
}

export function getJurisdictionsApi() {
  return request({
    url: `${APP_CONFIG.API_URL}/org/jurisdictions`,
    method: 'get',
    _needsToken: true
  })
}

export function getOrgTreeApi() {
  return request({
    url: `${APP_CONFIG.API_URL}/org/tree`,
    method: 'get',
    _needsToken: true
  })
}

export function addJurisdictionApi(orgCode, orgName) {
  return request({
    url: `${APP_CONFIG.API_URL}/org/jurisdiction`,
    method: 'post',
    data: { orgCode, orgName },
    _needsToken: true
  })
}

export function addBranchApi(branchCode, branchName, orgCode) {
  return request({
    url: `${APP_CONFIG.API_URL}/org/branch`,
    method: 'post',
    data: { branchCode, branchName, orgCode },
    _needsToken: true
  })
}

export function deleteJurisdictionApi(orgCode) {
  return request({
    url: `${APP_CONFIG.API_URL}/org/jurisdiction/${encodeURIComponent(orgCode)}`,
    method: 'delete',
    _needsToken: true
  })
}

export function updateJurisdictionApi(orgCode, orgName) {
  return request({
    url: `${APP_CONFIG.API_URL}/org/jurisdiction/${encodeURIComponent(orgCode)}`,
    method: 'put',
    data: { orgName },
    _needsToken: true
  })
}

export function updateBranchApi(branchCode, orgCode, branchName) {
  return request({
    url: `${APP_CONFIG.API_URL}/org/branch/${encodeURIComponent(branchCode)}/jurisdiction/${encodeURIComponent(orgCode)}`,
    method: 'put',
    data: { branchName },
    _needsToken: true
  })
}

export function deleteBranchApi(branchCode, orgCode) {
  return request({
    url: `${APP_CONFIG.API_URL}/org/branch/${encodeURIComponent(branchCode)}/jurisdiction/${encodeURIComponent(orgCode)}`,
    method: 'delete',
    _needsToken: true
  })
}

export function lookupOrgInGbaseApi(orgCode) {
  return request({
    url: `${APP_CONFIG.API_URL}/org/gbase-lookup`,
    method: 'get',
    params: { orgCode },
    _needsToken: true
  })
}

// ======================== 范围组 CRUD ========================

/** 获取全部范围组树（含成员和管理人员） */
export function getGroupTreeApi() {
  return request({
    url: `${APP_CONFIG.API_URL}/org/group/tree`,
    method: 'get',
    _needsToken: true
  })
}

/** 新建范围组 */
export function createGroupApi(groupName) {
  return request({
    url: `${APP_CONFIG.API_URL}/org/group`,
    method: 'post',
    data: { groupName },
    _needsToken: true
  })
}

/** 编辑范围组名称 */
export function updateGroupApi(groupCode, groupName) {
  return request({
    url: `${APP_CONFIG.API_URL}/org/group/${encodeURIComponent(groupCode)}`,
    method: 'put',
    data: { groupName },
    _needsToken: true
  })
}

/** 删除范围组 */
export function deleteGroupApi(groupCode) {
  return request({
    url: `${APP_CONFIG.API_URL}/org/group/${encodeURIComponent(groupCode)}`,
    method: 'delete',
    _needsToken: true
  })
}

// ======================== 组成员 CRUD ========================

/** 添加机构至范围组 */
export function addMemberApi(groupCode, orgCode, orgName) {
  return request({
    url: `${APP_CONFIG.API_URL}/org/group/${encodeURIComponent(groupCode)}/member`,
    method: 'post',
    data: { orgCode, orgName },
    _needsToken: true
  })
}

/** 从范围组移出机构 */
export function removeMemberApi(groupCode, orgCode) {
  return request({
    url: `${APP_CONFIG.API_URL}/org/group/${encodeURIComponent(groupCode)}/member/${encodeURIComponent(orgCode)}`,
    method: 'delete',
    _needsToken: true
  })
}

/** 设为管辖机构 */
export function setManagerOrgApi(groupCode, orgCode) {
  return request({
    url: `${APP_CONFIG.API_URL}/org/group/${encodeURIComponent(groupCode)}/member/${encodeURIComponent(orgCode)}/manager`,
    method: 'put',
    _needsToken: true
  })
}

/** 取消管辖机构 */
export function unsetManagerOrgApi(groupCode, orgCode) {
  return request({
    url: `${APP_CONFIG.API_URL}/org/group/${encodeURIComponent(groupCode)}/member/${encodeURIComponent(orgCode)}/manager`,
    method: 'delete',
    _needsToken: true
  })
}

// ======================== 管理人员 CRUD ========================

/** 添加管理人员 */
export function addGroupManagerApi(groupCode, ehrNo, userName) {
  return request({
    url: `${APP_CONFIG.API_URL}/org/group/${encodeURIComponent(groupCode)}/manager`,
    method: 'post',
    data: { ehrNo, userName },
    _needsToken: true
  })
}

/** 移除管理人员 */
export function removeGroupManagerApi(groupCode, ehrNo) {
  return request({
    url: `${APP_CONFIG.API_URL}/org/group/${encodeURIComponent(groupCode)}/manager/${encodeURIComponent(ehrNo)}`,
    method: 'delete',
    _needsToken: true
  })
}

// ======================== 查询辅助 ========================

/** 根据 ehrNo 查询人员姓名和所属机构 */
export function userLookupApi(ehrNo) {
  return request({
    url: `${APP_CONFIG.API_URL}/org/user-lookup`,
    method: 'get',
    params: { ehrNo },
    _needsToken: true
  })
}
