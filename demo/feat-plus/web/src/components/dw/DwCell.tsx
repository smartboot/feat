import type { DwColumn, DwRow } from "@/api/types"
import { Badge } from "@/components/ui/badge"

/**
 * 单元格渲染。运行时列表（DwList）与配置预览（DwPreview）共用同一份实现——
 * 预览如果另写一套，它就只能证明「长得差不多」，证明不了「配完就是长这样」。
 */
export function DwCell({ column, row }: { column: DwColumn; row: DwRow }) {
  const value = row[column.key]
  switch (column.format) {
    case "muted":
      return <span className="text-sm text-muted-foreground">{text(value)}</span>
    case "number":
      return <span className="text-sm tabular-nums">{text(value)}</span>
    case "badge": {
      const raw = text(value)
      const option = (column.options ?? []).find((item) => item.value === raw)
      return (
        <Badge variant={option?.variant ?? "secondary"}>
          <span className="size-1.5 rounded-full bg-current" />
          {option?.label ?? raw}
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
          {values.slice(0, 4).map((item) => (
            <Badge key={item} variant="secondary" className="text-[10px]">
              {item}
            </Badge>
          ))}
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
      return <span className="text-sm">{text(value)}</span>
  }
}

/** 行数据的值只可能是字符串 / 数字 / 布尔 / 字符串数组，统一转展示文本。 */
export function text(value: unknown): string {
  if (value == null) return ""
  if (Array.isArray(value)) return value.map(String).join(", ")
  return String(value)
}
