import request from '@/utils/request'
import { APP_CONFIG } from '@/config'

const BASE = `${APP_CONFIG.API_URL}/knowledge`

/** 知识条目列表（按标题聚合） */
export function listKnowledgeApi() {
  return request({
    url: `${BASE}/list`,
    method: 'get',
    _needsToken: true
  })
}

/** 查询某标题原文（编辑回显） */
export function getKnowledgeApi(title) {
  return request({
    url: `${BASE}/${encodeURIComponent(title)}`,
    method: 'get',
    _needsToken: true
  })
}

/** 新增文本知识条目 */
export function addKnowledgeApi(title, category, content) {
  return request({
    url: `${BASE}`,
    method: 'post',
    data: { title, category, content },
    _needsToken: true
  })
}

/** 上传文件导入（pdf/doc/docx/txt/md） */
export function importKnowledgeApi(file, category) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: `${BASE}/import`,
    method: 'post',
    params: { category },
    data: formData,
    _needsToken: true
  })
}

/** 编辑知识条目 */
export function updateKnowledgeApi(title, category, content) {
  return request({
    url: `${BASE}/${encodeURIComponent(title)}`,
    method: 'put',
    data: { category, content },
    _needsToken: true
  })
}

/** 删除知识条目 */
export function deleteKnowledgeApi(title) {
  return request({
    url: `${BASE}/${encodeURIComponent(title)}`,
    method: 'delete',
    _needsToken: true
  })
}
