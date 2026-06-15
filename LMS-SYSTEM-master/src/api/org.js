import request from '@/utils/request'
import { APP_CONFIG } from '@/config'

export function getRoleByOrgCodeApi(orgCode) {
  return request({
    url: `${APP_CONFIG.API_URL}/org/role`,
    method: 'get',
    params: { orgCode },
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
