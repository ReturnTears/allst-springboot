export const planLabel = {
  MONTHLY: '月订',
  QUARTERLY: '季订',
  YEARLY: '年订'
}

export const membershipStatusLabel = {
  ACTIVE: '在订',
  PAUSED: '暂停',
  CANCELLED: '已退订',
  EXPIRED: '已到期'
}

export const readerStatusLabel = {
  ACTIVE: '可用',
  DISABLED: '停用'
}

export function money(value) {
  return `¥${Number(value || 0).toFixed(2)}`
}

export function when(value) {
  if (!value) {
    return ''
  }
  return String(value).replace('T', ' ').slice(0, 16)
}
