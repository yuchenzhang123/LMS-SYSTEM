const SESSION_KEY = 'lms_session'
const SESSION_DURATION = 12 * 60 * 60 * 1000 // 12小时

export function setSession(userInfo) {
  localStorage.setItem(SESSION_KEY, JSON.stringify({
    userInfo,
    expiresAt: Date.now() + SESSION_DURATION
  }))
}

export function getSession() {
  try {
    const raw = localStorage.getItem(SESSION_KEY)
    if (!raw) return null
    const { userInfo, expiresAt } = JSON.parse(raw)
    if (Date.now() > expiresAt) {
      clearSession()
      return null
    }
    return userInfo
  } catch {
    clearSession()
    return null
  }
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY)
}
