export const formatDate = (value, fallback = '날짜 미정') => {
  if (!value) return fallback
  const [year, month, day] = String(value).slice(0, 10).split('-')
  return year && month && day ? `${year}.${month}.${day}` : String(value)
}

export const dateRange = (beginDate, endDate) => `${formatDate(beginDate)} – ${formatDate(endDate)}`

export const daysBetween = (beginDate, endDate) => {
  const begin = new Date(`${beginDate}T00:00:00`)
  const end = new Date(`${endDate}T00:00:00`)
  if (Number.isNaN(begin.getTime()) || Number.isNaN(end.getTime()) || begin > end) return []
  const days = []
  for (let date = begin, dayNo = 1; date <= end; date = new Date(date.getTime() + 86400000), dayNo += 1) {
    days.push({ dayNo, date: date.toISOString().slice(0, 10) })
  }
  return days
}

export const today = () => new Date().toISOString().slice(0, 10)
