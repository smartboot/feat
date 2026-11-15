import { useState } from "react"
import { Download, Plus, Trash2 } from "lucide-react"

import type { DwOption, DwVariant } from "@/api/types"
import { DwOptionImportDialog } from "@/components/dw/DwOptionImportDialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"

const VARIANTS: DwVariant[] = ["secondary", "success", "warning", "destructive", "outline"]

/**
 * DwOption[] 的行编辑器：badge 列的值映射、select 筛选 / 字段的选项、字段值域共用。
 * <p>
 * 值留空、值重复由调用方在提交时统一报错——这里逐行提示会随输入抖动。
 * <p>
 * 「从接口导入」放在这里而不是某个调用方：手抄映射会漂移（接口加一项，这里少一条，
 * 表现是那一格冒出原始编码），能同步的入口应该和映射编辑在一起，而不是藏在别处。
 */
export function DwOptionListEditor({
  options,
  onChange,
}: {
  options: DwOption[]
  onChange: (options: DwOption[]) => void
}) {
  const [importOpen, setImportOpen] = useState(false)

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
      <div className="flex flex-wrap gap-1.5">
        <Button
          variant="outline"
          size="sm"
          className="h-7 w-fit gap-1 text-xs"
          onClick={() => onChange([...options, { value: "", label: "", variant: "secondary" }])}
        >
          <Plus className="size-3" />
          添加选项
        </Button>
        <Button
          variant="outline"
          size="sm"
          className="h-7 w-fit gap-1 text-xs"
          title="从站内接口读一份字典，转成静态映射（渲染不发请求）"
          onClick={() => setImportOpen(true)}
        >
          <Download className="size-3" />
          从接口导入
        </Button>
      </div>

      <DwOptionImportDialog
        open={importOpen}
        onOpenChange={setImportOpen}
        existing={options}
        onImported={(next) => {
          onChange(next)
          setImportOpen(false)
        }}
      />
    </div>
  )
}
