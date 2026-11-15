import type { DwFormConfig, DwFormField } from "@/api/types"
import { MarkDot } from "@/components/dw/DwEditorBits"
import type { DwMarkLevel } from "@/components/dw/link"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Switch } from "@/components/ui/switch"
import { Textarea } from "@/components/ui/textarea"
import { cn } from "@/lib/utils"

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
  values: Record<string, string>
  onChange?: (key: string, value: string) => void
  /** 编辑态：immutable 字段变成只读 */
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
              </Label>
            )}
            {renderField({
              field,
              value: values[field.key] ?? "",
              id: `${idPrefix}-control-${field.key}-${index}`,
              update: onChange,
              editing,
              disabled,
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
}: {
  field: DwFormField
  value: string
  id: string
  update?: (key: string, value: string) => void
  editing: boolean
  disabled?: boolean
}) {
  // immutable = 创建可填、编辑只读（角色编码这类锚点字段）。
  const locked = Boolean(disabled || (field.immutable && editing))
  const set = (next: string) => update?.(field.key, next)

  switch (field.widget) {
    case "password":
      return (
        <Input
          id={id}
          type="password"
          value={value}
          disabled={locked}
          placeholder={field.placeholder}
          onChange={(event) => set(event.target.value)}
        />
      )
    case "textarea":
      return (
        <Textarea
          id={id}
          value={value}
          disabled={locked}
          placeholder={field.placeholder}
          onChange={(event) => set(event.target.value)}
        />
      )
    case "select":
      return (
        <Select value={value} onValueChange={set} disabled={locked}>
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
          <Label htmlFor={id} className="text-sm">
            {field.label}
            {field.required && <span className="text-destructive"> *</span>}
          </Label>
          <Switch
            id={id}
            checked={value === "1"}
            disabled={locked}
            onCheckedChange={(checked) => set(checked ? "1" : "0")}
          />
        </div>
      )
    case "span":
      return (
        <p className="min-h-9 rounded-md border border-input bg-muted/40 px-3 py-2 text-sm">
          {value || "—"}
        </p>
      )
    case "input":
    default:
      return (
        <Input
          id={id}
          value={value}
          disabled={locked}
          placeholder={field.placeholder}
          onChange={(event) => set(event.target.value)}
        />
      )
  }
}

/** 行取值转表单值：动态层的值统一按字符串编辑（switch 的 "1"/"0" 同此约定）。 */
export function fieldText(value: unknown): string {
  if (value == null) return ""
  return String(value)
}
