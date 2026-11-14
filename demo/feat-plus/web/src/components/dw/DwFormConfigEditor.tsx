import type { ReactNode } from "react"
import { ChevronDown, ChevronUp, Plus, Trash2 } from "lucide-react"

import type { DwFieldMeta, DwFormConfig, DwFormField, DwWidget } from "@/api/types"
import { Button } from "@/components/ui/button"
import { DwOptionListEditor } from "@/components/dw/DwOptionListEditor"
import { FieldKeySelect } from "@/components/dw/DwListConfigEditor"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Switch } from "@/components/ui/switch"

const WIDGETS: DwWidget[] = ["input", "password", "textarea", "select", "switch", "span"]
const TEXT_WIDGETS: DwWidget[] = ["input", "password", "textarea"]

/**
 * 页面配置的表单编辑器：字段 / 控件 / 校验规则按控件编辑。
 * 候选字段只列数据源的<b>可写</b>字段——只读字段配进表单保存时会被后端拒绝，
 * 这里直接不给选。
 */
export function DwFormConfigEditor({
  value,
  fields,
  onChange,
}: {
  value: DwFormConfig
  fields: DwFieldMeta[]
  onChange: (form: DwFormConfig) => void
}) {
  const writableFields = fields.filter((field) => field.writable)
  const writableKeys = new Set(writableFields.map((field) => field.key))

  function patch(next: Partial<DwFormConfig>) {
    onChange({ ...value, ...next })
  }

  function updateField(index: number, next: DwFormField) {
    patch({ fields: value.fields.map((field, i) => (i === index ? next : field)) })
  }

  function moveField(index: number, delta: number) {
    const target = index + delta
    if (target < 0 || target >= value.fields.length) return
    const moved = [...value.fields]
    const [item] = moved.splice(index, 1)
    moved.splice(target, 0, item)
    patch({ fields: moved })
  }

  const unknownKeys = value.fields
    .map((field) => field.key)
    .filter((key) => key !== "" && !writableKeys.has(key))

  return (
    <div className="grid gap-4">
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="grid gap-1.5">
          <FieldLabel>新建弹窗标题</FieldLabel>
          <Input
            className="h-8"
            value={value.createTitle}
            placeholder="新建"
            onChange={(event) => patch({ createTitle: event.target.value })}
          />
        </div>
        <div className="grid gap-1.5">
          <FieldLabel>编辑弹窗标题</FieldLabel>
          <Input
            className="h-8"
            value={value.editTitle}
            placeholder="编辑"
            onChange={(event) => patch({ editTitle: event.target.value })}
          />
        </div>
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <div className="grid gap-1.5">
          <FieldLabel>新建说明</FieldLabel>
          <Input
            className="h-8"
            value={value.createDescription}
            placeholder="弹窗里给用户看的提示"
            onChange={(event) => patch({ createDescription: event.target.value })}
          />
        </div>
        <div className="grid gap-1.5">
          <FieldLabel>编辑说明</FieldLabel>
          <Input
            className="h-8"
            value={value.editDescription}
            placeholder="弹窗里给用户看的提示"
            onChange={(event) => patch({ editDescription: event.target.value })}
          />
        </div>
      </div>

      <div className="grid gap-2">
        <div className="flex items-center justify-between gap-2">
          <Label>
            表单字段
            <span className="ml-1.5 text-xs font-normal text-muted-foreground">{value.fields.length} 项</span>
          </Label>
          <Button
            variant="outline"
            size="sm"
            className="h-7 gap-1 text-xs"
            onClick={() =>
              patch({
                fields: [
                  ...value.fields,
                  {
                    key: "",
                    label: "",
                    widget: "input",
                    required: false,
                    immutable: false,
                    placeholder: "",
                    defaultValue: "",
                    pattern: "",
                    patternError: "",
                    options: [],
                    span: 1,
                  },
                ],
              })
            }
          >
            <Plus className="size-3" />
            添加字段
          </Button>
        </div>

        <div className="grid gap-2">
          {value.fields.map((field, index) => (
            <div key={index} className="grid gap-2 rounded-lg border p-2.5">
              <div className="flex items-center justify-between">
                <span className="text-xs font-medium text-muted-foreground">字段 {index + 1}</span>
                <div className="flex items-center gap-0.5">
                  <Button
                    variant="ghost"
                    size="icon"
                    className="size-7"
                    aria-label="上移"
                    disabled={index === 0}
                    onClick={() => moveField(index, -1)}
                  >
                    <ChevronUp className="size-3.5" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="size-7"
                    aria-label="下移"
                    disabled={index === value.fields.length - 1}
                    onClick={() => moveField(index, 1)}
                  >
                    <ChevronDown className="size-3.5" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="size-7 text-destructive"
                    aria-label="删除"
                    onClick={() => patch({ fields: value.fields.filter((_, i) => i !== index) })}
                  >
                    <Trash2 className="size-3.5" />
                  </Button>
                </div>
              </div>

              <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
                <div className="grid gap-1">
                  <FieldLabel>字段 *</FieldLabel>
                  <FieldKeySelect
                    value={field.key}
                    fields={writableFields}
                    used={value.fields.map((item, i) => (i === index ? "" : item.key))}
                    onChange={(key) =>
                      updateField(index, {
                        ...field,
                        key,
                        label: field.label || writableFields.find((meta) => meta.key === key)?.label || "",
                      })
                    }
                  />
                </div>
                <div className="grid gap-1">
                  <FieldLabel>名称 *</FieldLabel>
                  <Input
                    className="h-8"
                    value={field.label}
                    placeholder="表单里显示的字段名"
                    onChange={(event) => updateField(index, { ...field, label: event.target.value })}
                  />
                </div>
                <div className="grid gap-1">
                  <FieldLabel>控件</FieldLabel>
                  <Select
                    value={field.widget}
                    onValueChange={(widget) => updateField(index, { ...field, widget: widget as DwWidget })}
                  >
                    <SelectTrigger className="h-8">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {WIDGETS.map((widget) => (
                        <SelectItem key={widget} value={widget}>
                          {widget}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="grid gap-1">
                  <FieldLabel>宽度</FieldLabel>
                  <Select
                    value={String(field.span ?? 1)}
                    onValueChange={(span) => updateField(index, { ...field, span: Number(span) as 1 | 2 })}
                  >
                    <SelectTrigger className="h-8">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="1">半宽</SelectItem>
                      <SelectItem value="2">全宽</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="flex flex-wrap items-center gap-3">
                <div className="flex items-center gap-1.5">
                  <Switch
                    id={`dw-field-required-${index}`}
                    checked={Boolean(field.required)}
                    disabled={field.widget === "span"}
                    onCheckedChange={(required) => updateField(index, { ...field, required })}
                  />
                  <Label htmlFor={`dw-field-required-${index}`} className="text-xs font-normal">
                    必填{field.widget === "span" ? "（展示字段不能必填）" : ""}
                  </Label>
                </div>
                <div className="flex items-center gap-1.5">
                  <Switch
                    id={`dw-field-immutable-${index}`}
                    checked={Boolean(field.immutable)}
                    onCheckedChange={(immutable) => updateField(index, { ...field, immutable })}
                  />
                  <Label htmlFor={`dw-field-immutable-${index}`} className="text-xs font-normal">
                    创建后锁定
                  </Label>
                </div>
              </div>

              {field.widget === "select" && (
                <div className="grid gap-1">
                  <FieldLabel>选项（select）</FieldLabel>
                  <DwOptionListEditor
                    options={field.options ?? []}
                    onChange={(options) => updateField(index, { ...field, options })}
                  />
                </div>
              )}

              {TEXT_WIDGETS.includes(field.widget) && (
                <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
                  <div className="grid gap-1">
                    <FieldLabel>占位提示</FieldLabel>
                    <Input
                      className="h-8"
                      value={field.placeholder ?? ""}
                      onChange={(event) => updateField(index, { ...field, placeholder: event.target.value })}
                    />
                  </div>
                  <div className="grid gap-1">
                    <FieldLabel>默认值</FieldLabel>
                    <Input
                      className="h-8"
                      value={field.defaultValue ?? ""}
                      onChange={(event) => updateField(index, { ...field, defaultValue: event.target.value })}
                    />
                  </div>
                  <div className="grid gap-1">
                    <FieldLabel>校验正则</FieldLabel>
                    <Input
                      className="h-8 font-mono text-xs"
                      value={field.pattern ?? ""}
                      placeholder="留空不校验"
                      onChange={(event) => updateField(index, { ...field, pattern: event.target.value })}
                    />
                  </div>
                  <div className="grid gap-1">
                    <FieldLabel>校验失败提示</FieldLabel>
                    <Input
                      className="h-8"
                      value={field.patternError ?? ""}
                      placeholder="格式不正确"
                      onChange={(event) => updateField(index, { ...field, patternError: event.target.value })}
                    />
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

      {unknownKeys.length > 0 && (
        <p className="text-xs text-destructive" role="alert">
          以下字段不在当前数据源的可写字段里（可能是刚切换了数据源），请重新选择：
          {unknownKeys.map((key) => ` ${key}`)}
        </p>
      )}
    </div>
  )
}

/** 行内小标签：视觉层级低于表单主 Label。 */
function FieldLabel({ children }: { children: ReactNode }) {
  return <span className="text-xs text-muted-foreground">{children}</span>
}
