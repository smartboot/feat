import { Plus, Trash2 } from "lucide-react"

import type { DwOption, DwVariant } from "@/api/types"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"

const VARIANTS: DwVariant[] = ["secondary", "success", "warning", "destructive", "outline"]

/**
 * DwOption[] 的行编辑器：badge 列的值映射、select 筛选 / 字段的选项共用。
 * <p>
 * 值留空、值重复由调用方在提交时统一报错——这里逐行提示会随输入抖动。
 */
export function DwOptionListEditor({
  options,
  onChange,
}: {
  options: DwOption[]
  onChange: (options: DwOption[]) => void
}) {
  function update(index: number, patch: Partial<DwOption>) {
    onChange(options.map((option, i) => (i === index ? { ...option, ...patch } : option)))
  }

  return (
    <div className="grid gap-1.5">
      {options.map((option, index) => (
        <div key={index} className="grid grid-cols-[1fr_1fr_auto_auto] items-center gap-1.5">
          <Input
            aria-label={`选项 ${index + 1} 的值`}
            className="h-8 text-xs"
            placeholder="值"
            value={option.value}
            onChange={(event) => update(index, { value: event.target.value })}
          />
          <Input
            aria-label={`选项 ${index + 1} 的文案`}
            className="h-8 text-xs"
            placeholder="显示文案"
            value={option.label}
            onChange={(event) => update(index, { label: event.target.value })}
          />
          <Select
            value={option.variant ?? "secondary"}
            onValueChange={(value) => update(index, { variant: value as DwVariant })}
          >
            <SelectTrigger aria-label={`选项 ${index + 1} 的样式`} className="h-8 w-[104px] text-xs">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {VARIANTS.map((variant) => (
                <SelectItem key={variant} value={variant}>
                  {variant}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button
            variant="ghost"
            size="icon"
            className="size-8"
            aria-label={`删除选项 ${index + 1}`}
            onClick={() => onChange(options.filter((_, i) => i !== index))}
          >
            <Trash2 className="size-3.5" />
          </Button>
        </div>
      ))}
      <Button
        variant="outline"
        size="sm"
        className="h-7 w-fit gap-1 text-xs"
        onClick={() => onChange([...options, { value: "", label: "", variant: "secondary" }])}
      >
        <Plus className="size-3" />
        添加选项
      </Button>
    </div>
  )
}
