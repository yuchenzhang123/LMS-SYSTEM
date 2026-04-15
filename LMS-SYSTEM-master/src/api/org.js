import request from '@/utils/request'

export function getRoleByOrgCodeApi(orgCode) {
  return request({
    url: `${process.env.VUE_APP_API_PREFIX}/org/role`,
    method: 'get',
    params: { orgCode },
    _needsToken: true
  })
}

export function getBranchesByOrgCodeApi(orgCode) {
  return request({
    url: `${process.env.VUE_APP_API_PREFIX}/org/branches`,
    method: 'get',
    params: { orgCode },
    _needsToken: true
  })
}

export function getJurisdictionsApi() {
  return request({
    url: `${process.env.VUE_APP_API_PREFIX}/org/jurisdictions`,
    method: 'get',
    _needsToken: true
  })
}

export function getOrgTreeApi() {
  return request({
    url: `${process.env.VUE_APP_API_PREFIX}/org/tree`,
    method: 'get',
    _needsToken: true
  })
}

export function addJurisdictionApi(orgCode, orgName) {
  return request({
    url: `${process.env.VUE_APP_API_PREFIX}/org/jurisdiction`,
    method: 'post',
    data: { orgCode, orgName },
    _needsToken: true
  })
}

export function addBranchApi(branchCode, branchName, orgCode) {
  return request({
    url: `${process.env.VUE_APP_API_PREFIX}/org/branch`,
    method: 'post',
    data: { branchCode, branchName, orgCode },
    _needsToken: true
  })
}

export function deleteJurisdictionApi(orgCode) {
  return request({
    url: `${process.env.VUE_APP_API_PREFIX}/org/jurisdiction/${orgCode}`,
    method: 'delete',
    _needsToken: true
  })
}

export function deleteBranchApi(branchCode) {
  return request({
    url: `${process.env.VUE_APP_API_PREFIX}/org/branch/${branchCode}`,
    method: 'delete',
    _needsToken: true
  })
}

export function lookupOrgInGbaseApi(orgCode) {
  return request({
    url: `${process.env.VUE_APP_API_PREFIX}/org/gbase-lookup`,
    method: 'get',
    params: { orgCode },
    _needsToken: true
  })
}
