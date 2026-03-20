export async function downloadFileByUrl({ url, fileName }) {
  if (!url) {
    throw new Error('下载地址为空')
  }
  const response = await fetch(url)
  if (!response.ok) {
    throw new Error(`下载失败(${response.status})`)
  }
  const blob = await response.blob()
  const objectUrl = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = fileName || 'material-file'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(objectUrl)
}

/**
 * 下载 Blob 数据
 * @param {Blob} blob - 要下载的 Blob 对象
 * @param {string} fileName - 文件名
 */
export function downloadBlob(blob, fileName) {
  if (!blob) {
    throw new Error('Blob 数据为空')
  }
  const objectUrl = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = fileName || 'material-file'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(objectUrl)
}

export async function safeDownloadFileByUrl({ url, fileName }) {
  try {
    await downloadFileByUrl({ url, fileName })
    return {
      success: true,
      message: ''
    }
  } catch (error) {
    return {
      success: false,
      message: error && error.message ? error.message : '导出失败，请稍后重试'
    }
  }
}
