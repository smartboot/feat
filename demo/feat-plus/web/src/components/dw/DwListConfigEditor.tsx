import { useState } from "react"
import { ChevronDown } from "lucide-react"

import type {
  DwAction,
  DwColumn,
  DwColumnFormat,
  DwFieldMeta,
  DwFilter,
  DwFilterWidget,
  DwListConfig,
  DwRowAction,
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
  MAX_ROW_ACTIONS,
  WIDTH_TIERS,
  columnFromField,
  defaultFormatFor,
  isKnownWidth,
  newRowAction,
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
  const rowActions = value.rowActions ?? []

  function patch(patch: Partial<DwListConfig>) {
    onChange({ ...value, ...patch })
  }

  /** 行操作是数组项，逐项打补丁；整表替换会把这行其他输入框的焦点状态一起换掉。 */
  function updateRowAction(index: number, patchAction: Partial<DwRowAction>) {
    patch({
      rowActions: rowActions.map((action, i) => (i === index ? { ...action, ...patchAction } : action)),
    })
  }

  function moveRowAction(index: number, delta: number) {
    const target = index + delta
    if (target < 0 || target >= rowActions.length) return
    const next = [...rowActions]
    const [moved] = next.splice(index, 1)
    next.splice(target, 0, moved)
    patch({ rowActions: next })
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
        title="自定义行操作"
        anchor="list-row-actions"
        count={rowActions.length}
        empty={
          writable
            ? "没有自定义按钮。需要「重置密码」「重新分配」这类对单行的操作时，加一个：它会出现在行操作菜单里，排在编辑 / 删除之后。"
            : "该数据源是只读的，不能加自定义行操作——行操作属于写动作，与编辑 / 删除同一条约束。"
        }
        onAdd={() => patch({ rowActions: [...rowActions, newRowAction()] })}
        addDisabled={!writable || rowActions.length >= MAX_ROW_ACTIONS}
        footer={
          rowActions.length >= MAX_ROW_ACTIONS
            ? `已到上限 ${MAX_ROW_ACTIONS} 个：再多就不该挤在一个下拉里，改做成工具栏的批量操作。`
            : undefined
        }
      >
        {rowActions.map((action, index) => (
          <ItemRow
            key={index}
            index={index}
            total={rowActions.length}
            title={action.label || `按钮 ${index + 1}`}
            subtitle={action.api || "未配置接口"}
            onMove={(delta) => moveRowAction(index, delta)}
            onRemove={() => patch({ rowActions: rowActions.filter((_, i) => i !== index) })}
          >
            <div className="grid gap-2 sm:grid-cols-2">
              <div className="grid gap-1">
                <FieldLabel>按钮名称</FieldLabel>
                <Input
                  className="h-8"
                  maxLength={12}
                  value={action.label}
                  placeholder="重置密码"
                  onChange={(event) => updateRowAction(index, { label: event.target.value })}
                />
              </div>
              <div className="grid gap-1">
                <FieldLabel>接口路径</FieldLabel>
                <Input
                  className="h-8 font-mono text-xs"
                  value={action.api}
                  placeholder="/api/users/reset-password"
                  onChange={(event) => updateRowAction(index, { api: event.target.value.trim() })}
                />
              </div>
            </div>
            <div className="grid gap-1">
              <FieldLabel>提示框（可选）</FieldLabel>
              <Input
                className="h-8"
                maxLength={120}
                value={action.confirm ?? ""}
                placeholder="留空：点一下就直接执行"
                onChange={(event) => updateRowAction(index, { confirm: event.target.value })}
              />
            </div>
          </ItemRow>
        ))}
      </Section>
      {rowActions.length > 0 && (
        <Hint>
          调用方式固定：<code>POST {"{接口}"}</code>，请求体是{" "}
          <code>{`{"${value.rowKey || "id"}": 主键值}`}</code>——主键由「行主键字段」决定，不用在按钮上再写一遍。
          能不能点仍受当前账号写权限过滤；接口自己的鉴权由目标接口负责，配置只存路径。
        </Hint>
      )}

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
                    used={usedByOthers(value.filters, index)}
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
                      // 换控件时清掉另一支的配置：否则会存下「既是下拉又是日期范围」的混合体，
                      // 换回来时又带着上次的选项，配置页说不清它现在是什么。
                      updateFilter(index, {
                        ...filter,
                        widget: widget as DwFilterWidget,
                        endKey: widget === "dateRange" ? (filter.endKey ?? "") : "",
                        options: widget === "select" ? (filter.options ?? []) : [],
                        optionsApi: widget === "select" ? (filter.optionsApi ?? "") : "",
                        optionValueField: widget === "select" ? (filter.optionValueField ?? "") : "",
                        optionLabelField: widget === "select" ? (filter.optionLabelField ?? "") : "",
                      })
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
              {filter.widget === "input" && (
                <div className="grid gap-1">
                  <FieldLabel>占位提示</FieldLabel>
                  <Input
                    className="h-8"
                    value={filter.placeholder ?? ""}
                    placeholder={filter.label || "输入关键字"}
                    onChange={(event) => updateFilter(index, { ...filter, placeholder: event.target.value })}
                  />
                </div>
              )}
              {filter.widget === "dateRange" && (
                <div className="grid gap-1">
                  <FieldLabel>结束字段</FieldLabel>
                  <FieldKeySelect
                    value={filter.endKey ?? ""}
                    fields={fields}
                    used={reservedForEnd(value.filters, index)}
                    onChange={(endKey) => updateFilter(index, { ...filter, endKey })}
                    ariaLabel="日期范围结束字段"
                  />
                  <Hint>
                    日期范围占两个筛选键：起始是 <code>{filter.key || "（未选）"}</code>，结束是{" "}
                    <code>{filter.endKey || "（未选）"}</code>。数据源分别读这两个键，不做拼接解析。
                  </Hint>
                </div>
              )}
              {filter.widget === "select" && (
                <div className="grid gap-2">
                  <div className="grid gap-1">
                    <FieldLabel>选项来源</FieldLabel>
                    <Select
                      value={filter.optionsApi ? "api" : "static"}
                      onValueChange={(source) =>
                        updateFilter(index, {
                          ...filter,
                          // 两个来源互斥：留着一半的旧配置，取数时就要猜「到底哪个算」。
                          options: source === "static" ? (filter.options ?? []) : [],
                          optionsApi: source === "api" ? (filter.optionsApi ?? "") : "",
                          optionValueField: source === "api" ? (filter.optionValueField ?? "") : "",
                          optionLabelField: source === "api" ? (filter.optionLabelField ?? "") : "",
                        })
                      }
                    >
                      <SelectTrigger className="h-8">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="static">写死的选项</SelectItem>
                        <SelectItem value="api">从接口按需取</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  {filter.optionsApi ? (
                    <>
                      <div className="grid gap-1">
                        <FieldLabel>接口路径</FieldLabel>
                        <Input
                          className="h-8"
                          value={filter.optionsApi}
                          placeholder="/api/activities/operators"
                          onChange={(event) =>
                            updateFilter(index, { ...filter, optionsApi: event.target.value })
                          }
                        />
                      </div>
                      <div className="grid gap-1">
                        <FieldLabel>取值字段 / 显示字段</FieldLabel>
                        <div className="flex items-center gap-2">
                          <Input
                            className="h-8"
                            value={filter.optionValueField ?? ""}
                            placeholder="id"
                            aria-label="取值字段"
                            onChange={(event) =>
                              updateFilter(index, { ...filter, optionValueField: event.target.value })
                            }
                          />
                          <span className="shrink-0 text-xs text-muted-foreground">/</span>
                          <Input
                            className="h-8"
                            value={filter.optionLabelField ?? ""}
                            placeholder="name"
                            aria-label="显示字段"
                            onChange={(event) =>
                              updateFilter(index, { ...filter, optionLabelField: event.target.value })
                            }
                          />
                        </div>
                      </div>
                      <Hint>
                        只允许站内 <code>/api/</code> 路径，接口返回对象数组。选项在打开页面时现取，
                        不写进配置——角色改名、新增部门之后不用重新保存这一页。
                      </Hint>
                    </>
                  ) : (
                    <div className="grid gap-1">
                      <FieldLabel>选项（下拉里除「全部」之外的取值）</FieldLabel>
                      <DwOptionListEditor
                        options={filter.options ?? []}
                        onChange={(options) => updateFilter(index, { ...filter, options })}
                      />
                    </div>
                  )}
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
 * 被「其他筛选」占用的字段键，含它们的日期范围结束键。
 * 漏掉 endKey 的后果是两个筛选能配成同一个结束字段，保存时被后端拒掉——
 * 前端能选、后端才报错，是这一层最该提前挡住的错配。
 */
function usedByOthers(filters: DwFilter[], index: number): string[] {
  const used: string[] = []
  filters.forEach((filter, i) => {
    if (i === index) return
    if (filter.key) used.push(filter.key)
    if (filter.widget === "dateRange" && filter.endKey) used.push(filter.endKey)
  })
  return used
}

/** 结束字段额外要排除当前筛选自己的起始键：起止配成同一个字段没有意义。 */
function reservedForEnd(filters: DwFilter[], index: number): string[] {
  const used = usedByOthers(filters, index)
  const self = filters[index]?.key
  if (self) used.push(self)
  return used
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
