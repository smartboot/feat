import type { DwColumn, DwOption, DwRow } from "@/api/types"
import { Badge } from "@/components/ui/badge"

/**
 * 单元格渲染。运行时列表（DwList）与配置预览（DwPreview）共用同一份实现——
 * 预览如果另写一套，它就只能证明「长得差不多」，证明不了「配完就是长这样」。
 */
export function DwCell({ column, row }: { column: DwColumn; row: DwRow }) {
  const value = row[column.key]
  switch (column.format) {
    case "muted":
      return <span className="text-sm text-muted-foreground">{cellText(value, column)}</span>
    case "number":
      return <span className="text-sm tabular-nums">{cellText(value, column)}</span>
    case "badge": {
      const option = findOption(column, text(value))
      return (
        <Badge variant={option?.variant ?? "secondary"}>
          <span className="size-1.5 rounded-full bg-current" />
          {cellText(value, column)}
        </Badge>
      )
    }
    case "tags": {
      const values = Array.isArray(value) ? value.map(text) : value ? [text(value)] : []
      if (values.length === 0) {
        return <span className="text-xs text-muted-foreground">—</span>
      }
      return (
        <div className="flex flex-wrap gap-1">
          {values.slice(0, 4).map((item, index) => {
            // 多值字段逐值翻译：权限点列是 ["role:read", "role:write"]，
            // 配了映射就该显示「角色查看 / 角色编辑」，而不是原样吐编码。
            const option = findOption(column, item)
            return (
              <Badge key={`${item}-${index}`} variant={option?.variant ?? "secondary"} className="text-[10px]">
                {option?.label || item}
              </Badge>
            )
          })}
          {values.length > 4 && (
            <Badge variant="outline" className="text-[10px]">
              +{values.length - 4}
            </Badge>
          )}
        </div>
      )
    }
    case "text":
    default:
      return <span className="text-sm">{cellText(value, column)}</span>
  }
}

/** 行数据的值只可能是字符串 / 数字 / 布尔 / 字符串数组，统一转展示文本。 */
export function text(value: unknown): string {
  if (value == null) return ""
  if (Array.isArray(value)) return value.map(String).join(", ")
  return String(value)
}

/** 命中列的映射表就返回它，未命中返回 undefined。 */
export function findOption(column: DwColumn, raw: string): DwOption | undefined {
  if (!raw) return undefined
  return (column.options ?? []).find((option) => option.value === raw)
}

/**
 * 一个单元格最终显示什么字。三件事按顺序，先到先得：
 * <ol>
 *   <li>值映射：{@code ROLE_ADMIN} → 超管。这是「翻译」，回答「这个编码是什么意思」；</li>
 *   <li>值格式化：日期 / 千分位。这是「排版」，回答「同一个值怎么写更好读」；</li>
 *   <li>原值。</li>
 * </ol>
 * 映射优先是因为它表达的是业务语义，格式化只是写法。两个都配的时候，
 * 把编码格式化一遍再去找映射必然找不到，所以顺序不能反。
 */
export function cellText(value: unknown, column: DwColumn): string {
  const raw = text(value)
  const option = findOption(column, raw)
  if (option) return option.label || raw
  return formatValue(raw, column.valueFormat)
}

/** 空串表示原样；解析不出来也原样返回——显示成空比显示成「Invalid Date」更难查。 */
export function formatValue(raw: string, valueFormat?: string): string {
  if (!raw || !valueFormat) return raw
  switch (valueFormat) {
    case "date":
    case "datetime": {
      const date = parseDate(raw)
      if (!date) return raw
      const day = `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
      return valueFormat === "date" ? day : `${day} ${pad(date.getHours())}:${pad(date.getMinutes())}`
    }
    case "thousands":
      return groupThousands(raw)
    default:
      return raw
  }
}

function pad(value: number): string {
  return value < 10 ? `0${value}` : String(value)
}

/**
 * 自己分组而不是 `Number.toLocaleString`：后者依赖运行环境的 ICU 数据，
 * 分组符会跟着 locale 变，配置页看到的效果和线上未必一致。
 */
function groupThousands(raw: string): string {
  const trimmed = raw.trim()
  if (!/^-?\d+(\.\d+)?$/.test(trimmed)) return raw
  const negative = trimmed.startsWith("-")
  const body = negative ? trimmed.slice(1) : trimmed
  const [intPart, decPart] = body.split(".")
  const grouped = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, ",")
  return `${negative ? "-" : ""}${grouped}${decPart ? `.${decPart}` : ""}`
}

/**
 * 把原始值解析成 Date。
 * <p>
 * 纯数字只当时戳（13 位毫秒、10 位秒）：把 {@code 20261115} 当毫秒会得到 1970 年。
 * 不带时区的 {@code 2026-11-15} 按本地时间构造——交给 {@code new Date()} 会按 UTC 零点算，
 * 东八区就显示成 08:00，日期列集体错一天。
 */
function parseDate(raw: string): Date | null {
  const trimmed = raw.trim()
  if (!trimmed) return null
  if (/^\d+$/.test(trimmed)) {
    const num = Number(trimmed)
    const millis = trimmed.length >= 13 ? num : trimmed.length >= 10 ? num * 1000 : NaN
    if (!Number.isFinite(millis)) return null
    const date = new Date(millis)
    return Number.isNaN(date.getTime()) ? null : date
  }
  const hasZone = /(?:Z|[+-]\d{2}:?\d{2})$/.test(trimmed)
  if (!hasZone) {
    const match = /^(\d{4})-(\d{1,2})-(\d{1,2})(?:[ T](\d{1,2}):(\d{2})(?::\d{2})?)?/.exec(trimmed)
    if (match) {
      const [, year, month, day, hour, minute] = match
      return new Date(Number(year), Number(month) - 1, Number(day), Number(hour ?? 0), Number(minute ?? 0))
    }
  }
  const parsed = new Date(trimmed)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}
