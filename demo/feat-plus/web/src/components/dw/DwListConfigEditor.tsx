import { useState } from "react"
import { ChevronDown } from "lucide-react"

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
import { FieldLabel, Hint, ItemRow, Section } from "@/components/dw/DwEditorBits"
import { DwOptionListEditor } from "@/components/dw/DwOptionListEditor"
import type { EditorLink } from "@/components/dw/link"
import {
  ACTIONS,
  ACTION_HINTS,
  ACTION_LABELS,
  COLUMN_FORMATS,
  COLUMN_FORMAT_HINTS,
  COLUMN_FORMAT_LABELS,
  FIELD_TYPE_LABELS,
  FILTER_WIDGET_LABELS,
  FILTER_WIDGETS,
  WIDTH_TIERS,
  columnFromField,
  defaultFormatFor,
  isKnownWidth,
} from "@/components/dw/meta"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { cn } from "@/lib/utils"

const WRITE_ACTIONS: DwAction[] = ["create", "edit", "delete"]

/**
 * 列表视图编辑器。
 *
 * 三处刻意的设计：
 * ① 字段只能从数据源声明里选，已用过的置灰——「配置漂移」与「同字段两列」在这里就没有输入面；
 * ② 列名默认继承数据源字段标签，改过的才单独存：同一个字段在列表叫「角色名称」、
 *    在表单叫「名称」的不一致，大多是因为两处都要手填一遍；
 * ③ 宽度给档位而不是 Tailwind 类名——存量值（w-36 之类）识别成「自定义」原样保留，不覆盖。
 */
export function DwListConfigEditor({
  value,
  fields,
  writable,
  onChange,
  link,
}: {
  value: DwListConfig
  fields: DwFieldMeta[]
  /** 只读数据源：写动作只能看不能勾 */
  writable: boolean
  onChange: (list: DwListConfig) => void
  /** 与右侧预览的联动：悬停 / 聚焦某一条时，预览里对应位置高亮 */
  link?: EditorLink
}) {
  const fieldKeys = new Set(fields.map((field) => field.key))
  // 覆盖列之前先确认：现有列的格式 / 宽度 / 选项映射是手工调过的，静默覆盖等于丢工作。
  const [confirmGenerate, setConfirmGenerate] = useState(false)
  const [advancedOpen, setAdvancedOpen] = useState(Boolean(value.dataApi?.query || value.dataApi?.delete))

  function patch(patch: Partial<DwListConfig>) {
    onChange({ ...value, ...patch })
  }

  /** 通道覆盖的字段级更新：dataApi 整体存在或不存在都好处理。 */
  function patchDataApi(patch: Partial<NonNullable<DwListConfig["dataApi"]>>) {
    onChange({ ...value, dataApi: { ...value.dataApi, ...patch } })
  }

  function toggleAction(action: DwAction) {
    const active = value.actions.includes(action)
    patch({
      actions: active ? value.actions.filter((item) => item !== action) : [...value.actions, action],
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

  /** 按数据源字段一键生成列：格式按声明类型映射，列名继承字段标签。 */
  function generateColumns() {
    patch({ columns: fields.map(columnFromField) })
    setConfirmGenerate(false)
  }

  const unknownKeys = [
    ...value.columns.map((column) => column.key),
    ...value.filters.map((filter) => filter.key),
  ].filter((key) => key !== "" && !fieldKeys.has(key))

  const unusedFields = fields.filter(
    (field) =>
      !value.columns.some((column) => column.key === field.key) &&
      !value.filters.some((filter) => filter.key === field.key),
  )

  return (
    <div className="grid gap-5">
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="grid gap-1.5" id="row-key">
          <Label>行主键字段</Label>
          <FieldKeySelect value={value.rowKey} fields={fields} onChange={(key) => patch({ rowKey: key })} />
          <Hint>行操作、编辑回填、删除都按它定位。通常是 id。</Hint>
        </div>
        <div className="grid gap-1.5" id="page-size">
          <Label>每页条数</Label>
          <Input
            type="number"
            min={1}
            max={200}
            value={value.pageSize}
            onChange={(event) => patch({ pageSize: Number(event.target.value) || 0 })}
          />
          <Hint>1–200，用户可以在列表底部临时改成其他档位。</Hint>
        </div>
      </div>

      <div className="grid gap-2" id="list-actions">
        <Label>工具栏与行操作</Label>
        <div className="flex flex-wrap gap-1.5">
          {ACTIONS.map((action) => {
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
                title={disabled ? "只读数据源不能配置写动作" : ACTION_HINTS[action]}
              >
                {ACTION_LABELS[action]}
              </Button>
            )
          })}
        </div>
        <Hint>
          {writable
            ? "新建 / 编辑 / 删除会同时受当前账号权限过滤：没有写权限的人看不到这些按钮。"
            : "该数据源是只读的，新建 / 编辑 / 删除不可勾选（保存时后端也会拒绝）。"}
        </Hint>
      </div>

      <Section
        title="列"
        anchor="list-columns"
        count={value.columns.length}
        empty="还没有列。用「按字段生成」按数据源字段一次建好，再逐列调格式。"
        onGenerate={() => (value.columns.length === 0 ? generateColumns() : setConfirmGenerate(true))}
        onAdd={() => patch({ columns: [...value.columns, { key: "", label: "", format: "text", width: "", options: [] }] })}
        footer={
          unusedFields.length > 0 && value.columns.length > 0
            ? `还有 ${unusedFields.length} 个字段没有出现在列表里：${unusedFields
                .slice(0, 6)
                .map((field) => field.label)
                .join("、")}${unusedFields.length > 6 ? " 等" : ""}`
            : undefined
        }
      >
        {value.columns.map((column, index) => {
          const meta = fields.find((field) => field.key === column.key)
          const usableKeys = value.columns.map((item, i) => (i === index ? "" : item.key))
          return (
            <ItemRow
              key={index}
              target={{ kind: "column", index }}
              link={link}
              index={index}
              total={value.columns.length}
              title={column.label || meta?.label || `第 ${index + 1} 列`}
              subtitle={column.key ? `${column.key} · ${COLUMN_FORMAT_LABELS[column.format]}` : "未选择字段"}
              onMove={(delta) => moveColumn(index, delta)}
              onRemove={() => patch({ columns: value.columns.filter((_, i) => i !== index) })}
            >
              <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
                <div className="grid gap-1">
                  <FieldLabel>字段</FieldLabel>
                  <FieldKeySelect
                    value={column.key}
                    fields={fields}
                    used={usableKeys}
                    onChange={(key) => {
                      const nextMeta = fields.find((field) => field.key === key)
                      updateColumn(index, {
                        ...column,
                        key,
                        // 列名还是空的或是上一个字段的标签时，跟着换；手工改过就保留。
                        label:
                          !column.label || column.label === meta?.label
                            ? nextMeta?.label ?? ""
                            : column.label,
                        format: meta ? column.format : defaultFormatFor(nextMeta?.type ?? "string"),
                      })
                    }}
                  />
                </div>
                <div className="grid gap-1">
                  <FieldLabel>列名</FieldLabel>
                  <Input
                    className="h-8"
                    value={column.label}
                    placeholder={meta?.label || "表头文案"}
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
                          {COLUMN_FORMAT_LABELS[format]}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="grid gap-1">
                  <FieldLabel>宽度</FieldLabel>
                  <WidthSelect
                    value={column.width}
                    onChange={(width) => updateColumn(index, { ...column, width })}
                  />
                </div>
              </div>
              <Hint>{COLUMN_FORMAT_HINTS[column.format]}</Hint>
              {column.format === "badge" && (
                <div className="grid gap-1">
                  <FieldLabel>值 → 文案映射（不配的话只显示原始值）</FieldLabel>
                  <DwOptionListEditor
                    options={column.options ?? []}
                    onChange={(options) => updateColumn(index, { ...column, options })}
                  />
                </div>
              )}
            </ItemRow>
          )
        })}
      </Section>

      <Section
        title="筛选"
        anchor="list-filters"
        count={value.filters.length}
        empty="没有筛选项时，列表工具栏不出现筛选区。"
        onAdd={() =>
          patch({
            filters: [...value.filters, { key: "", label: "", widget: "input", placeholder: "", options: [] }],
          })
        }
      >
        {value.filters.map((filter, index) => {
          const meta = fields.find((field) => field.key === filter.key)
          return (
            <ItemRow
              key={index}
              target={{ kind: "filter", index }}
              link={link}
              index={index}
              total={value.filters.length}
              title={filter.label || meta?.label || `筛选 ${index + 1}`}
              subtitle={filter.key ? `${filter.key} · ${FILTER_WIDGET_LABELS[filter.widget]}` : "未选择字段"}
              onMove={(delta) => moveFilter(index, delta)}
              onRemove={() => patch({ filters: value.filters.filter((_, i) => i !== index) })}
            >
              <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
                <div className="grid gap-1">
                  <FieldLabel>字段</FieldLabel>
                  <FieldKeySelect
                    value={filter.key}
                    fields={fields}
                    used={value.filters.map((item, i) => (i === index ? "" : item.key))}
                    onChange={(key) => {
                      const nextMeta = fields.find((field) => field.key === key)
                      updateFilter(index, {
                        ...filter,
                        key,
                        label:
                          !filter.label || filter.label === meta?.label
                            ? nextMeta?.label ?? ""
                            : filter.label,
                      })
                    }}
                  />
                </div>
                <div className="grid gap-1">
                  <FieldLabel>名称</FieldLabel>
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
                    onValueChange={(widget) =>
                      updateFilter(index, { ...filter, widget: widget as "input" | "select" })
                    }
                  >
                    <SelectTrigger className="h-8">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {FILTER_WIDGETS.map((widget) => (
                        <SelectItem key={widget} value={widget}>
                          {FILTER_WIDGET_LABELS[widget]}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>
              {filter.widget === "input" ? (
                <div className="grid gap-1">
                  <FieldLabel>占位提示</FieldLabel>
                  <Input
                    className="h-8"
                    value={filter.placeholder ?? ""}
                    placeholder={filter.label || "输入关键字"}
                    onChange={(event) => updateFilter(index, { ...filter, placeholder: event.target.value })}
                  />
                </div>
              ) : (
                <div className="grid gap-1">
                  <FieldLabel>选项（下拉里除「全部」之外的取值）</FieldLabel>
                  <DwOptionListEditor
                    options={filter.options ?? []}
                    onChange={(options) => updateFilter(index, { ...filter, options })}
                  />
                </div>
              )}
            </ItemRow>
          )
        })}
      </Section>

      <div className="grid gap-2 rounded-lg border border-dashed p-3">
        <button
          type="button"
          className="flex items-center justify-between text-left"
          onClick={() => setAdvancedOpen((open) => !open)}
          aria-expanded={advancedOpen}
        >
          <span className="text-sm font-medium">高级：数据接口覆盖</span>
          <ChevronDown className={cn("size-4 text-muted-foreground transition-transform", advancedOpen && "rotate-180")} />
        </button>
        {advancedOpen && (
          <div className="grid gap-2">
            <Hint>
              留空走内置 /api/dw 通道；填了则该操作改走自定义接口。接口须返回与数据源声明一致的行结构，
              权限由目标接口自己校验。查询 GET <code>{"{path}?page&pageSize&filter"}</code>，删除 DELETE{" "}
              <code>{"{path}/{id}"}</code>。保存与详情不支持覆盖——表单写路径牵涉校验与审计，不开旁路。
            </Hint>
            <div className="grid gap-2 sm:grid-cols-2">
              <div className="grid gap-1" id="data-api-query">
                <FieldLabel>查询接口</FieldLabel>
                <Input
                  className="h-8 font-mono text-xs"
                  value={value.dataApi?.query ?? ""}
                  placeholder="/api/..."
                  onChange={(event) => patchDataApi({ query: event.target.value.trim() })}
                />
              </div>
              <div className="grid gap-1" id="data-api-delete">
                <FieldLabel>删除接口</FieldLabel>
                <Input
                  className="h-8 font-mono text-xs"
                  value={value.dataApi?.delete ?? ""}
                  placeholder="/api/..."
                  onChange={(event) => patchDataApi({ delete: event.target.value.trim() })}
                />
              </div>
            </div>
          </div>
        )}
      </div>

      {unknownKeys.length > 0 && (
        <p className="rounded-md border border-destructive/30 bg-destructive/5 p-2 text-xs text-destructive" role="alert">
          以下字段不在当前数据源的声明里（可能是切换过数据源），保存会被拒绝：
          {unknownKeys.map((key) => ` ${key}`)}。请到「字段」里用「迁移到新数据源」清理。
        </p>
      )}

      <AlertDialog open={confirmGenerate} onOpenChange={setConfirmGenerate}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>按字段重新生成列？</AlertDialogTitle>
            <AlertDialogDescription>
              现有 {value.columns.length} 列的格式、宽度与状态映射是手工配置的，重新生成会全部覆盖。
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

/** 宽度档位选择。识别不了的存量值（`w-36` 之外的自定义类名）走「自定义」并原样保留。 */
function WidthSelect({ value, onChange }: { value?: string; onChange: (value: string) => void }) {
  const [forceCustom, setForceCustom] = useState(false)
  const known = isKnownWidth(value)
  const custom = forceCustom || !known

  if (custom) {
    return (
      <div className="flex items-center gap-1">
        <Input
          className="h-8 font-mono text-xs"
          value={value ?? ""}
          placeholder="留空为自适应"
          onChange={(event) => onChange(event.target.value)}
        />
        <Button
          variant="ghost"
          size="sm"
          className="h-8 shrink-0 px-2 text-[11px]"
          onClick={() => {
            setForceCustom(false)
            onChange("")
          }}
        >
          档位
        </Button>
      </div>
    )
  }

  return (
    <Select
      value={value || "__auto"}
      onValueChange={(next) => {
        if (next === "__custom") {
          setForceCustom(true)
          return
        }
        onChange(next === "__auto" ? "" : next)
      }}
    >
      <SelectTrigger className="h-8">
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        {WIDTH_TIERS.map((tier) => (
          <SelectItem key={tier.value || "auto"} value={tier.value || "__auto"}>
            {tier.label}
          </SelectItem>
        ))}
        <SelectItem value="__custom">自定义…</SelectItem>
      </SelectContent>
    </Select>
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
            {field.label}
            <span className="ml-1 text-[10px] text-muted-foreground">
              {field.key} · {FIELD_TYPE_LABELS[field.type] ?? field.type}
            </span>
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}
