import { Check, Lock } from "lucide-react"

import type { DwFormConfig, DwFormField, DwFormValue, DwOption } from "@/api/types"
import { MarkDot } from "@/components/dw/DwEditorBits"
import { isFieldWritable, writeModeOf } from "@/components/dw/meta"
import type { DwMarkLevel } from "@/components/dw/link"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Switch } from "@/components/ui/switch"
import { Textarea } from "@/components/ui/textarea"
import { cn } from "@/lib/utils"
import { useRemoteOptions, type RemoteOption } from "@/services/queries"

/**
 * 表单字段渲染。运行时弹窗（DwFormDialog）与配置预览（DwPreview）共用。
 *
 * 字段顺序、宽度、控件全部来自配置——这里不做任何「按 key 猜控件」的兜底，
 * 猜出来的东西在预览里对、上线后不对，是最难查的一类问题。
 */
/** 预览联动：把某个字段圈出来，并允许点它跳到配置。 */
export interface FormFieldsLink {
  /** 需要圈出来的字段下标 */
  index?: number | null
  /** 字段下标 → 问题级别。编辑区与预览标同一个颜色，才指向同一个原因。 */
  marks?: Record<number, DwMarkLevel>
  onSelect?: (index: number) => void
  onHover?: (index: number | null) => void
}

export function DwFormFields({
  form,
  values,
  onChange,
  editing,
  disabled,
  idPrefix = "dw-field",
  fieldErrors,
  link,
}: {
  form: DwFormConfig
  values: Record<string, DwFormValue>
  onChange?: (key: string, value: DwFormValue) => void
  /** 编辑态：createOnly 字段变成只读；readonly 字段任何态都只读 */
  editing: boolean
  /** 预览态：全部只读 */
  disabled?: boolean
  idPrefix?: string
  /** 按字段下标给的行内错误：错误挂在出错的那一行，而不是一句飘在底部的文案 */
  fieldErrors?: Record<number, string>
  link?: FormFieldsLink
}) {
  return (
    <div className="grid gap-4 sm:grid-cols-2">
      {form.fields.map((field, index) => {
        const focused = link?.index === index
        const mark = link?.marks?.[index]
        // 只读的原因要写在字段上：光把控件变灰，用户会以为是加载失败或者权限不足。
        const mode = writeModeOf(field)
        const lockNote = mode === "readonly" ? "始终只读" : mode === "createOnly" && editing ? "创建后不可改" : ""
        return (
          <div
            key={`${field.key}-${index}`}
            id={`${idPrefix}-${index}`}
            data-preview-focus={focused ? `field-${index}` : undefined}
            className={cn(
              "relative grid gap-2 rounded-md transition-shadow",
              (field.span ?? 1) === 2 && "sm:col-span-2",
              focused && "ring-2 ring-primary/30",
            )}
          >
            {field.widget !== "switch" && (
              <Label htmlFor={`${idPrefix}-control-${field.key}-${index}`} className="flex items-center gap-1.5">
                <span>
                  {field.label}
                  {field.required && <span className="text-destructive"> *</span>}
                </span>
                {/* 开关的标题画在控件里，标记点贴不到行首，留给发布检查列表去说 */}
                {mark && <MarkDot level={mark} />}
                {lockNote && <ReadonlyNote text={lockNote} />}
              </Label>
            )}
            {renderField({
              field,
              value: values[field.key] ?? (field.multiple ? [] : ""),
              id: `${idPrefix}-control-${field.key}-${index}`,
              update: onChange,
              editing,
              disabled,
              lockNote,
            })}
            {fieldErrors?.[index] && (
              <p className="text-xs text-destructive" role="alert">
                {fieldErrors[index]}
              </p>
            )}
            {/*
              预览里的控件是 disabled 的，浏览器不会从 disabled 控件派发 click，
              所以定位热区单独盖一层：既不改变控件外观，也保证「点哪都能跳去配置」。
            */}
            {link?.onSelect && (
              <button
                type="button"
                aria-label={`定位到表单字段「${field.label || `第 ${index + 1} 项`}」`}
                title="跳到这个字段的配置"
                className="absolute inset-0 rounded-md"
                onClick={() => link.onSelect?.(index)}
                onMouseEnter={() => link.onHover?.(index)}
                onMouseLeave={() => link.onHover?.(null)}
              />
            )}
          </div>
        )
      })}
      {form.fields.length === 0 && (
        <p className="text-sm text-muted-foreground sm:col-span-2">还没有配置表单字段。</p>
      )}
    </div>
  )
}

function renderField({
  field,
  value,
  id,
  update,
  editing,
  disabled,
  lockNote,
}: {
  field: DwFormField
  value: DwFormValue
  id: string
  update?: (key: string, value: DwFormValue) => void
  editing: boolean
  disabled?: boolean
  lockNote?: string
}) {
  // 只读 = 预览态全禁 + 配置的写入策略。两处规则同源，预览才不会「看着能改、上线不能改」。
  const locked = Boolean(disabled || !isFieldWritable(field, editing))
  const set = (next: string) => update?.(field.key, next)
  const text = typeof value === "string" ? value : ""

  // 多选走独立的勾选组：它要拉远程选项（hook 不能在这个函数里调），语义也和下拉不同。
  if (field.widget === "select" && field.multiple) {
    return (
      <MultiSelectField
        field={field}
        value={Array.isArray(value) ? value : []}
        locked={locked}
        onChange={(next) => update?.(field.key, next)}
      />
    )
  }

  switch (field.widget) {
    case "password":
      return (
        <Input
          id={id}
          type="password"
          value={text}
          disabled={locked}
          placeholder={field.placeholder}
          onChange={(event) => set(event.target.value)}
        />
      )
    case "textarea":
      return (
        <Textarea
          id={id}
          value={text}
          disabled={locked}
          placeholder={field.placeholder}
          onChange={(event) => set(event.target.value)}
        />
      )
    case "select":
      return (
        <Select value={text} onValueChange={set} disabled={locked}>
          <SelectTrigger id={id}>
            <SelectValue placeholder={field.placeholder || "请选择"} />
          </SelectTrigger>
          <SelectContent>
            {(field.options ?? []).map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label || option.value}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      )
    case "switch":
      return (
        <div className="flex items-center justify-between rounded-lg border px-3 py-2">
          <Label htmlFor={id} className="flex items-center gap-1.5 text-sm">
            <span>
              {field.label}
              {field.required && <span className="text-destructive"> *</span>}
            </span>
            {lockNote && <ReadonlyNote text={lockNote} />}
          </Label>
          <Switch
            id={id}
            checked={text === "1"}
            disabled={locked}
            onCheckedChange={(checked) => set(checked ? "1" : "0")}
          />
        </div>
      )
    case "span":
      return (
        <p className="min-h-9 rounded-md border border-input bg-muted/40 px-3 py-2 text-sm">
          {text || "—"}
        </p>
      )
    case "input":
    default:
      return (
        <Input
          id={id}
          value={text}
          disabled={locked}
          placeholder={field.placeholder}
          onChange={(event) => set(event.target.value)}
        />
      )
  }
}

/**
 * 多选勾选组：按分组字段分块，组标题带全选 / 清空。
 *
 * 为什么不复用下拉：多选下拉在「权限点有二十几个、需要分组扫一眼」的场景下不可用——
 * 用户看不到已选了什么，也没法整组勾选。企业里需要多选的地方几乎都是这种字典。
 *
 * 值就是字符串数组，不拼逗号串：拼接会让「值里含逗号」变成一条永远查不出来的边界。
 */
function MultiSelectField({
  field,
  value,
  locked,
  onChange,
}: {
  field: DwFormField
  value: string[]
  locked: boolean
  onChange: (next: string[]) => void
}) {
  const remote = useRemoteOptions(
    field.optionsApi ?? "",
    field.optionValueField || "value",
    field.optionLabelField || "label",
    field.optionGroupField || "",
    field.optionGroupLabelField || "label",
  )
  const options: RemoteOption[] = field.optionsApi
    ? remote.data ?? []
    : (field.options ?? []).map((option: DwOption) => ({
        value: option.value,
        label: option.label || option.value,
        group: "",
        groupLabel: "",
      }))

  const selected = new Set(value)
  const groups = groupOptions(options)

  if (field.optionsApi && remote.isLoading) {
    return <p className="min-h-9 rounded-md border border-dashed px-3 py-2 text-sm text-muted-foreground">正在加载选项...</p>
  }
  if (field.optionsApi && remote.isError) {
    return (
      <p className="min-h-9 rounded-md border border-destructive/40 px-3 py-2 text-sm text-destructive">
        选项加载失败，请检查选项接口
      </p>
    )
  }
  if (options.length === 0) {
    return <p className="min-h-9 rounded-md border border-dashed px-3 py-2 text-sm text-muted-foreground">没有可选项。</p>
  }

  const toggle = (key: string) => {
    if (locked) return
    const next = new Set(value)
    if (next.has(key)) next.delete(key)
    else next.add(key)
    // 保持选项原有顺序，避免每次勾选都把提交值顺序打乱（审计日志里看得出差别）。
    onChange(options.filter((option) => next.has(option.value)).map((option) => option.value))
  }

  const toggleGroup = (group: RemoteOption[]) => {
    if (locked) return
    const keys = group.map((option) => option.value)
    const allChecked = keys.every((key) => selected.has(key))
    const next = new Set(value)
    for (const key of keys) {
      if (allChecked) next.delete(key)
      else next.add(key)
    }
    onChange(options.filter((option) => next.has(option.value)).map((option) => option.value))
  }

  return (
    <div className={cn("grid gap-3 rounded-lg border p-3", locked && "opacity-70")}>
      {groups.map((group) => {
        const allChecked = group.items.every((option) => selected.has(option.value))
        return (
          <div key={group.key} className="grid gap-1.5">
            {group.label && (
              <div className="flex items-center justify-between">
                <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{group.label}</p>
                <button
                  type="button"
                  disabled={locked}
                  className="text-xs text-primary hover:underline disabled:cursor-not-allowed disabled:opacity-50"
                  onClick={() => toggleGroup(group.items)}
                >
                  {allChecked ? "清空本组" : "全选本组"}
                </button>
              </div>
            )}
            <div className="grid gap-1.5 sm:grid-cols-2">
              {group.items.map((option) => {
                const checked = selected.has(option.value)
                return (
                  <button
                    key={option.value}
                    type="button"
                    role="checkbox"
                    aria-checked={checked}
                    disabled={locked}
                    onClick={() => toggle(option.value)}
                    className={cn(
                      "flex items-center gap-2 rounded-md border px-2.5 py-1.5 text-left text-sm transition-colors",
                      checked ? "border-primary/40 bg-primary/5" : "hover:bg-muted/60",
                      locked && "cursor-not-allowed",
                    )}
                  >
                    <span
                      className={cn(
                        "flex size-4 shrink-0 items-center justify-center rounded border",
                        checked ? "border-primary bg-primary text-primary-foreground" : "border-input",
                      )}
                    >
                      {checked && <Check className="size-3" aria-hidden />}
                    </span>
                    <span className="truncate">{option.label}</span>
                  </button>
                )
              })}
            </div>
          </div>
        )
      })}
    </div>
  )
}

/** 按分组字段切块，保持接口返回的原始顺序（分组顺序就是模块顺序，重排会打乱阅读习惯）。 */
function groupOptions(options: RemoteOption[]): Array<{ key: string; label: string; items: RemoteOption[] }> {
  const groups = new Map<string, { key: string; label: string; items: RemoteOption[] }>()
  for (const option of options) {
    const existing = groups.get(option.group)
    if (existing) {
      existing.items.push(option)
      continue
    }
    groups.set(option.group, { key: option.group || "__all__", label: option.groupLabel, items: [option] })
  }
  return Array.from(groups.values())
}

/** 只读标记：小锁 + 原因。视觉上比灰掉控件更明确——用户知道「这是设计如此」，而不是坏了。 */
function ReadonlyNote({ text }: { text: string }) {
  return (
    <span
      className="inline-flex shrink-0 items-center gap-0.5 rounded bg-muted px-1 text-[10px] font-normal leading-4 text-muted-foreground"
      title={`该字段${text}，保存时服务端会保留原值`}
    >
      <Lock className="size-2.5" aria-hidden />
      {text}
    </span>
  )
}

/** 行取值转表单值：动态层的值统一按字符串编辑（switch 的 "1"/"0" 同此约定）。 */
export function fieldText(value: unknown): string {
  if (value == null) return ""
  return String(value)
}

/**
 * 行数据 → 表单初值。多选字段统一成数组。
 *
 * 行数据里的多选值可能是数组（数据源原样返回 `List`），也可能是逗号串（老接口），
 * 两种都收下——但提交时一律是数组，拼接只在「读旧数据」这一侧出现，不影响保存契约。
 */
export function initialFormValue(field: DwFormField, row: Record<string, unknown> | null): DwFormValue {
  if (!row) {
    if (field.multiple) {
      return (field.defaultValue ?? "")
        .split(",")
        .map((item) => item.trim())
        .filter(Boolean)
    }
    return field.defaultValue ?? ""
  }
  const raw = row[field.key]
  if (field.multiple) {
    if (Array.isArray(raw)) return raw.map((item) => String(item))
    if (raw == null || raw === "") return []
    return String(raw)
      .split(",")
      .map((item) => item.trim())
      .filter(Boolean)
  }
  return fieldText(raw)
}
