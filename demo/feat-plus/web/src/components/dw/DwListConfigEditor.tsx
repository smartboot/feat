import type { ReactNode } from "react"
import { useState } from "react"
import { ChevronDown, ChevronUp, Plus, Trash2, Wand2 } from "lucide-react"

import type {
  DwAction,
  DwColumn,
  DwColumnFormat,
  DwFieldMeta,
  DwFilter,
  DwListConfig,
} from "@/api/types"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Button } from "@/components/ui/button"
import { DwOptionListEditor } from "@/components/dw/DwOptionListEditor"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { cn } from "@/lib/utils"

const COLUMN_FORMATS: DwColumnFormat[] = ["text", "muted", "badge", "tags", "number"]
const WRITE_ACTIONS: DwAction[] = ["create", "edit", "delete"]
const ALL_ACTIONS: DwAction[] = ["create", "edit", "delete", "refresh"]

/**
 * 页面配置的列表编辑器：列 / 筛选 / 动作 / 分页全部按控件编辑，
 * 字段 key 只能从数据源的字段声明里选——「配置漂移」在这里就没有输入面。
 * 提交前由 PageManagePage 做镜像校验，后端 PageService 仍是最终闸门。
 */
export function DwListConfigEditor({
  value,
  fields,
  writable,
  onChange,
}: {
  value: DwListConfig
  fields: DwFieldMeta[]
  /** 只读数据源：写动作只能看不能勾 */
  writable: boolean
  onChange: (list: DwListConfig) => void
}) {
  const fieldKeys = new Set(fields.map((field) => field.key))
  // 覆盖列之前先确认：现有列的 format / width / 选项映射是手工调过的，静默覆盖等于丢工作。
  const [confirmGenerate, setConfirmGenerate] = useState(false)

  function patch(patch: Partial<DwListConfig>) {
    onChange({ ...value, ...patch })
  }

  function toggleAction(action: DwAction) {
    const active = value.actions.includes(action)
    patch({
      actions: active
        ? value.actions.filter((item) => item !== action)
        : [...value.actions, action],
    })
  }

  function updateColumn(index: number, next: DwColumn) {
    patch({ columns: value.columns.map((column, i) => (i === index ? next : column)) })
  }

  function moveColumn(index: number, delta: number) {
    const target = index + delta
    if (target < 0 || target >= value.columns.length) return
    const columns = [...value.columns]
    const [moved] = columns.splice(index, 1)
    columns.splice(target, 0, moved)
    patch({ columns })
  }

  function updateFilter(index: number, next: DwFilter) {
    patch({ filters: value.filters.map((filter, i) => (i === index ? next : filter)) })
  }

  function moveFilter(index: number, delta: number) {
    const target = index + delta
    if (target < 0 || target >= value.filters.length) return
    const filters = [...value.filters]
    const [moved] = filters.splice(index, 1)
    filters.splice(target, 0, moved)
    patch({ filters })
  }

  /** 按数据源字段一键生成列：text/number/tags 按声明类型映射。已有列时先确认再覆盖。 */
  function generateColumns() {
    const columns: DwColumn[] = fields.map((field) => ({
      key: field.key,
      label: field.label,
      format: field.type === "number" ? "number" : field.type === "list" ? "tags" : "text",
      width: "",
      options: [],
    }))
    patch({ columns })
    setConfirmGenerate(false)
  }

  const unknownKeys = [
    ...value.columns.map((column) => column.key),
    ...value.filters.map((filter) => filter.key),
  ].filter((key) => key !== "" && !fieldKeys.has(key))

  return (
    <div className="grid gap-4">
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="grid gap-2">
          <Label>行主键字段</Label>
          <FieldKeySelect
            value={value.rowKey}
            fields={fields}
            onChange={(key) => patch({ rowKey: key })}
          />
        </div>
        <div className="grid gap-2">
          <Label htmlFor="dw-list-pagesize">每页条数</Label>
          <Input
            id="dw-list-pagesize"
            type="number"
            min={1}
            max={200}
            value={value.pageSize}
            onChange={(event) => patch({ pageSize: Number(event.target.value) || 0 })}
          />
        </div>
      </div>

      <div className="grid gap-2">
        <Label>按钮动作</Label>
        <div className="flex flex-wrap gap-1.5">
          {ALL_ACTIONS.map((action) => {
            const disabled = !writable && WRITE_ACTIONS.includes(action)
            const active = value.actions.includes(action)
            return (
              <Button
                key={action}
                type="button"
                variant="outline"
                size="sm"
                className={cn("h-7 text-xs", active && "border-primary bg-primary/10 text-primary")}
                disabled={disabled}
                onClick={() => toggleAction(action)}
                title={disabled ? "只读数据源不能配置写动作" : undefined}
              >
                {action}
              </Button>
            )
          })}
        </div>
        {!writable && (
          <p className="text-xs text-muted-foreground">该数据源是只读的，create / edit / delete 不可勾选。</p>
        )}
      </div>

      <Section
        title="列"
        count={value.columns.length}
        onGenerate={() => (value.columns.length === 0 ? generateColumns() : setConfirmGenerate(true))}
        onAdd={() =>
          patch({ columns: [...value.columns, { key: "", label: "", format: "text", width: "", options: [] }] })
        }
      >
        {value.columns.map((column, index) => (
          <ItemRow
            key={index}
            index={index}
            total={value.columns.length}
            label={`列 ${index + 1}`}
            onMove={(delta) => moveColumn(index, delta)}
            onRemove={() => patch({ columns: value.columns.filter((_, i) => i !== index) })}
          >
            <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
              <div className="grid gap-1">
                <FieldLabel>字段 *</FieldLabel>
                <FieldKeySelect
                  value={column.key}
                  fields={fields}
                  used={value.columns.map((item, i) => (i === index ? "" : item.key))}
                  onChange={(key) =>
                    updateColumn(index, {
                      ...column,
                      key,
                      // 选字段时 label 为空则带出字段名，填过的不覆盖。
                      label: column.label || fields.find((field) => field.key === key)?.label || "",
                    })
                  }
                />
              </div>
              <div className="grid gap-1">
                <FieldLabel>列名 *</FieldLabel>
                <Input
                  className="h-8"
                  value={column.label}
                  placeholder="表头文案"
                  onChange={(event) => updateColumn(index, { ...column, label: event.target.value })}
                />
              </div>
              <div className="grid gap-1">
                <FieldLabel>格式</FieldLabel>
                <Select
                  value={column.format}
                  onValueChange={(format) => updateColumn(index, { ...column, format: format as DwColumnFormat })}
                >
                  <SelectTrigger className="h-8">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {COLUMN_FORMATS.map((format) => (
                      <SelectItem key={format} value={format}>
                        {format}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="grid gap-1">
                <FieldLabel>宽度 class</FieldLabel>
                <Input
                  className="h-8"
                  value={column.width ?? ""}
                  placeholder="如 w-36，留空自适应"
                  onChange={(event) => updateColumn(index, { ...column, width: event.target.value })}
                />
              </div>
            </div>
            {column.format === "badge" && (
              <div className="grid gap-1">
                <FieldLabel>值 → 文案映射（badge）</FieldLabel>
                <DwOptionListEditor
                  options={column.options ?? []}
                  onChange={(options) => updateColumn(index, { ...column, options })}
                />
              </div>
            )}
          </ItemRow>
        ))}
      </Section>

      <Section
        title="筛选"
        count={value.filters.length}
        onAdd={() =>
          patch({ filters: [...value.filters, { key: "", label: "", widget: "input", placeholder: "", options: [] }] })
        }
      >
        {value.filters.map((filter, index) => (
          <ItemRow
            key={index}
            index={index}
            total={value.filters.length}
            label={`筛选 ${index + 1}`}
            onMove={(delta) => moveFilter(index, delta)}
            onRemove={() => patch({ filters: value.filters.filter((_, i) => i !== index) })}
          >
            <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
              <div className="grid gap-1">
                <FieldLabel>字段 *</FieldLabel>
                <FieldKeySelect
                  value={filter.key}
                  fields={fields}
                  used={value.filters.map((item, i) => (i === index ? "" : item.key))}
                  onChange={(key) =>
                    updateFilter(index, {
                      ...filter,
                      key,
                      label: filter.label || fields.find((field) => field.key === key)?.label || "",
                    })
                  }
                />
              </div>
              <div className="grid gap-1">
                <FieldLabel>名称 *</FieldLabel>
                <Input
                  className="h-8"
                  value={filter.label}
                  placeholder="筛选项文案"
                  onChange={(event) => updateFilter(index, { ...filter, label: event.target.value })}
                />
              </div>
              <div className="grid gap-1">
                <FieldLabel>控件</FieldLabel>
                <Select
                  value={filter.widget}
                  onValueChange={(widget) => updateFilter(index, { ...filter, widget: widget as "input" | "select" })}
                >
                  <SelectTrigger className="h-8">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="input">input（模糊）</SelectItem>
                    <SelectItem value="select">select（精确）</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              {filter.widget === "input" && (
                <div className="grid gap-1">
                  <FieldLabel>占位提示</FieldLabel>
                  <Input
                    className="h-8"
                    value={filter.placeholder ?? ""}
                    onChange={(event) => updateFilter(index, { ...filter, placeholder: event.target.value })}
                  />
                </div>
              )}
            </div>
            {filter.widget === "select" && (
              <div className="grid gap-1">
                <FieldLabel>选项（select 精确匹配）</FieldLabel>
                <DwOptionListEditor
                  options={filter.options ?? []}
                  onChange={(options) => updateFilter(index, { ...filter, options })}
                />
              </div>
            )}
          </ItemRow>
        ))}
      </Section>

      {unknownKeys.length > 0 && (
        <p className="text-xs text-destructive" role="alert">
          以下字段不在当前数据源的声明里（可能是刚切换了数据源），请重新选择或用「按字段生成」重建：
          {unknownKeys.map((key) => ` ${key}`)}
        </p>
      )}

      <AlertDialog open={confirmGenerate} onOpenChange={setConfirmGenerate}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>按字段重新生成列？</AlertDialogTitle>
            <AlertDialogDescription>
              现有 {value.columns.length} 列的格式、宽度与 badge 映射是手工配置的，重新生成会全部覆盖。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>取消</AlertDialogCancel>
            <AlertDialogAction onClick={generateColumns}>重新生成</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}

/**
 * 字段下拉：候选来自数据源声明，已用在别处的 key 置灰，杜绝「重复字段」的输入面。
 * 供列表与表单编辑器共用。
 */
export function FieldKeySelect({
  value,
  fields,
  used = [],
  onChange,
  ariaLabel,
}: {
  value: string
  fields: DwFieldMeta[]
  /** 已被其他行占用的 key（当前行自己的 key 不算） */
  used?: string[]
  onChange: (key: string) => void
  ariaLabel?: string
}) {
  const usedSet = new Set(used.filter(Boolean))
  return (
    <Select value={value || undefined} onValueChange={onChange}>
      <SelectTrigger aria-label={ariaLabel} className="h-8">
        <SelectValue placeholder="选择字段" />
      </SelectTrigger>
      <SelectContent>
        {fields.map((field) => (
          <SelectItem key={field.key} value={field.key} disabled={usedSet.has(field.key)}>
            {field.label}（{field.key}）
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}

// ---------------------------------------------------------------------------
// 私有布局辅助
// ---------------------------------------------------------------------------

function Section({
  title,
  count,
  onGenerate,
  onAdd,
  children,
}: {
  title: string
  count: number
  onGenerate?: () => void
  onAdd: () => void
  children: ReactNode
}) {
  return (
    <div className="grid gap-2">
      <div className="flex items-center justify-between gap-2">
        <Label>
          {title}
          <span className="ml-1.5 text-xs font-normal text-muted-foreground">{count} 项</span>
        </Label>
        <div className="flex items-center gap-1.5">
          {onGenerate && (
            <Button variant="outline" size="sm" className="h-7 gap-1 text-xs" onClick={onGenerate}>
              <Wand2 className="size-3" />
              按字段生成
            </Button>
          )}
          <Button variant="outline" size="sm" className="h-7 gap-1 text-xs" onClick={onAdd}>
            <Plus className="size-3" />
            添加
          </Button>
        </div>
      </div>
      <div className="grid gap-2">{children}</div>
    </div>
  )
}

function ItemRow({
  label,
  index,
  total,
  onMove,
  onRemove,
  children,
}: {
  label: string
  index: number
  total: number
  onMove: (delta: number) => void
  onRemove: () => void
  children: ReactNode
}) {
  return (
    <div className="grid gap-2 rounded-lg border p-2.5">
      <div className="flex items-center justify-between">
        <span className="text-xs font-medium text-muted-foreground">{label}</span>
        <div className="flex items-center gap-0.5">
          <Button
            variant="ghost"
            size="icon"
            className="size-7"
            aria-label="上移"
            disabled={index === 0}
            onClick={() => onMove(-1)}
          >
            <ChevronUp className="size-3.5" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            className="size-7"
            aria-label="下移"
            disabled={index === total - 1}
            onClick={() => onMove(1)}
          >
            <ChevronDown className="size-3.5" />
          </Button>
          <Button variant="ghost" size="icon" className="size-7 text-destructive" aria-label="删除" onClick={onRemove}>
            <Trash2 className="size-3.5" />
          </Button>
        </div>
      </div>
      {children}
    </div>
  )
}

/** 行内小标签：视觉层级低于表单主 Label。 */
function FieldLabel({ children }: { children: ReactNode }) {
  return <span className="text-xs text-muted-foreground">{children}</span>
}
