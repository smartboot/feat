import { useState } from "react"
import { ChevronDown, Plus, Wand2 } from "lucide-react"

import type { DwFieldMeta, DwFormConfig, DwFormField, DwWidget, DwWriteMode } from "@/api/types"
import { Button } from "@/components/ui/button"
import { FieldLabel, ItemRow } from "@/components/dw/DwEditorBits"
import { DwOptionListEditor } from "@/components/dw/DwOptionListEditor"
import { FieldKeySelect } from "@/components/dw/DwListConfigEditor"
import type { EditorLink } from "@/components/dw/link"
import {
  FIELD_TYPE_LABELS,
  FORM_WIDGET_HINTS,
  FORM_WIDGET_LABELS,
  WIDGETS,
  WRITE_MODES,
  WRITE_MODE_HINTS,
  WRITE_MODE_LABELS,
  fieldFromMeta,
  writeModeOf,
} from "@/components/dw/meta"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Switch } from "@/components/ui/switch"
import { cn } from "@/lib/utils"

const TEXT_WIDGETS: DwWidget[] = ["input", "password", "textarea"]

/**
 * 表单编辑器。
 *
 * 候选字段只列数据源的<b>可写</b>字段——只读字段配进表单保存时会被后端拒绝，
 * 这里直接不给选；「按可写字段生成」把这一步也省掉。
 *
 * 每个字段的二级配置按控件类型收起：文本类才需要占位 / 默认值 / 正则，
 * 下拉才需要选项，开关只需要默认开还是关。一屏里塞满永远用不到的输入框，
 * 是这类配置页最容易失去维护性的地方。
 */
export function DwFormConfigEditor({
  value,
  fields,
  onChange,
  link,
}: {
  value: DwFormConfig
  fields: DwFieldMeta[]
  onChange: (form: DwFormConfig) => void
  /** 与右侧预览的联动：悬停 / 聚焦某个字段时，预览弹窗里对应字段高亮 */
  link?: EditorLink
}) {
  const writable = fields.filter((field) => field.writable)
  const writableKeys = new Set(writable.map((field) => field.key))
  const [copyOpen, setCopyOpen] = useState(false)

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

  const unused = writable.filter((field) => !value.fields.some((item) => item.key === field.key))

  return (
    <div className="grid gap-5">
      <div className="grid gap-2 rounded-lg border">
        <button
          type="button"
          className="flex items-center justify-between px-3 py-2 text-left"
          onClick={() => setCopyOpen((open) => !open)}
          aria-expanded={copyOpen}
        >
          <div>
            <p className="text-sm font-medium">弹窗文案</p>
            <p className="text-[11px] text-muted-foreground">
              「{value.createTitle || "新建"}」/「{value.editTitle || "编辑"}」
              {value.createDescription || value.editDescription
                ? ` · ${value.createDescription || value.editDescription}`
                : ""}
            </p>
          </div>
          <ChevronDown className={cn("size-4 text-muted-foreground transition-transform", copyOpen && "rotate-180")} />
        </button>
        {copyOpen && (
          <div className="grid gap-3 border-t p-3">
            <div className="grid gap-2 sm:grid-cols-2">
              <div className="grid gap-1">
                <FieldLabel>新建弹窗标题</FieldLabel>
                <Input
                  className="h-8"
                  value={value.createTitle}
                  placeholder="新建"
                  onChange={(event) => patch({ createTitle: event.target.value })}
                />
              </div>
              <div className="grid gap-1">
                <FieldLabel>编辑弹窗标题</FieldLabel>
                <Input
                  className="h-8"
                  value={value.editTitle}
                  placeholder="编辑"
                  onChange={(event) => patch({ editTitle: event.target.value })}
                />
              </div>
            </div>
            <div className="grid gap-2 sm:grid-cols-2">
              <div className="grid gap-1">
                <FieldLabel>新建说明</FieldLabel>
                <Input
                  className="h-8"
                  value={value.createDescription}
                  placeholder="标题下方的一行提示，可留空"
                  onChange={(event) => patch({ createDescription: event.target.value })}
                />
              </div>
              <div className="grid gap-1">
                <FieldLabel>编辑说明</FieldLabel>
                <Input
                  className="h-8"
                  value={value.editDescription}
                  placeholder="标题下方的一行提示，可留空"
                  onChange={(event) => patch({ editDescription: event.target.value })}
                />
              </div>
            </div>
          </div>
        )}
      </div>

      <div className="grid gap-2" id="form-fields">
        <div className="flex items-center justify-between gap-2">
          <Label>
            表单字段
            <span className="ml-1.5 text-xs font-normal text-muted-foreground">{value.fields.length} 项</span>
          </Label>
          <div className="flex items-center gap-1.5">
            <Button
              variant="outline"
              size="sm"
              className="h-7 gap-1 text-xs"
              onClick={() => patch({ fields: writable.map(fieldFromMeta) })}
              title="按数据源的可写字段生成，会覆盖现有字段"
            >
              <Wand2 className="size-3" />
              按字段生成
            </Button>
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
                      writeMode: "always",
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
        </div>

        {value.fields.length === 0 ? (
          <p className="rounded-md border border-dashed p-3 text-xs text-muted-foreground">
            还没有表单字段。列表若开了「新建 / 编辑」，这里必须有字段，否则弹窗是空的。
          </p>
        ) : (
          <div className="grid gap-2">
            {value.fields.map((field, index) => {
              const meta = writable.find((item) => item.key === field.key)
              return (
                <ItemRow
                  key={index}
                  target={{ kind: "field", index }}
                  link={link}
                  index={index}
                  total={value.fields.length}
                  title={field.label || meta?.label || `字段 ${index + 1}`}
                  subtitle={`${field.key ? `${field.key} · ${FORM_WIDGET_LABELS[field.widget]}` : "未选择字段"}${
                    field.required ? " · 必填" : ""
                  }`}
                  onMove={(delta) => moveField(index, delta)}
                  onRemove={() => patch({ fields: value.fields.filter((_, i) => i !== index) })}
                >

                  <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
                    <div className="grid gap-1">
                      <FieldLabel>字段</FieldLabel>
                      <FieldKeySelect
                        value={field.key}
                        fields={writable}
                        used={value.fields.map((item, i) => (i === index ? "" : item.key))}
                        onChange={(key) => {
                          const nextMeta = writable.find((item) => item.key === key)
                          updateField(index, {
                            ...field,
                            key,
                            label:
                              !field.label || field.label === meta?.label
                                ? nextMeta?.label ?? ""
                                : field.label,
                          })
                        }}
                      />
                    </div>
                    <div className="grid gap-1">
                      <FieldLabel>名称</FieldLabel>
                      <Input
                        className="h-8"
                        value={field.label}
                        placeholder={meta?.label || "表单里显示的字段名"}
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
                              {FORM_WIDGET_LABELS[widget]}
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
                          <SelectItem value="2">整行</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                  </div>

                  <p className="text-[11px] text-muted-foreground">{FORM_WIDGET_HINTS[field.widget]}</p>

                  <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
                    <div className="flex items-center gap-1.5">
                      <Switch
                        id={`dw-field-required-${index}`}
                        checked={Boolean(field.required)}
                        disabled={field.widget === "span" || writeModeOf(field) === "readonly"}
                        onCheckedChange={(required) => updateField(index, { ...field, required })}
                      />
                      <Label htmlFor={`dw-field-required-${index}`} className="text-xs font-normal">
                        必填
                      </Label>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <Label htmlFor={`dw-field-write-${index}`} className="text-xs font-normal">
                        写入
                      </Label>
                      <Select
                        value={writeModeOf(field)}
                        disabled={field.widget === "span"}
                        onValueChange={(next) => {
                          const writeMode = next as DwWriteMode
                          // immutable 是给老配置的兼容字段，跟着 writeMode 一起写，避免两处不一致。
                          updateField(index, { ...field, writeMode, immutable: writeMode !== "always" })
                        }}
                      >
                        <SelectTrigger id={`dw-field-write-${index}`} className="h-7 w-[124px] text-xs">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          {WRITE_MODES.map((mode) => (
                            <SelectItem key={mode} value={mode}>
                              {WRITE_MODE_LABELS[mode]}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                    <span className="text-[10px] text-muted-foreground">
                      {field.widget === "span" ? "只读展示控件不参与写入" : WRITE_MODE_HINTS[writeModeOf(field)]}
                    </span>
                    {meta && (
                      <span className="text-[10px] text-muted-foreground">
                        数据源类型：{FIELD_TYPE_LABELS[meta.type] ?? meta.type}
                        {meta.writable ? "" : "（只读）"}
                      </span>
                    )}
                  </div>

                  {field.widget === "select" && (
                    <div className="grid gap-1">
                      <FieldLabel>选项</FieldLabel>
                      <DwOptionListEditor
                        options={field.options ?? []}
                        onChange={(options) => updateField(index, { ...field, options })}
                      />
                    </div>
                  )}

                  {field.widget === "switch" && (
                    <div className="grid gap-1">
                      <FieldLabel>默认状态</FieldLabel>
                      <Select
                        value={field.defaultValue === "1" ? "1" : field.defaultValue === "0" ? "0" : "unset"}
                        onValueChange={(next) =>
                          updateField(index, { ...field, defaultValue: next === "unset" ? "" : next })
                        }
                      >
                        <SelectTrigger className="h-8 w-[140px]">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="unset">不预置</SelectItem>
                          <SelectItem value="1">默认打开</SelectItem>
                          <SelectItem value="0">默认关闭</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                  )}

                  {TEXT_WIDGETS.includes(field.widget) && (
                    <div className="grid gap-2 sm:grid-cols-2">
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
                          placeholder="留空不校验，如 ^[a-z0-9_-]+$"
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
                </ItemRow>
              )
            })}
          </div>
        )}

        {unused.length > 0 && value.fields.length > 0 && (
          <p className="text-[11px] text-muted-foreground">
            可写但没进表单的字段：{unused.map((field) => field.label).join("、")}——保存后这些字段无法被填写。
          </p>
        )}
      </div>

      {unknownKeys.length > 0 && (
        <p className="rounded-md border border-destructive/30 bg-destructive/5 p-2 text-xs text-destructive" role="alert">
          以下字段不在当前数据源的可写字段里（可能是切换过数据源）：
          {unknownKeys.map((key) => ` ${key}`)}。保存会被拒绝，请重新选择或删除。
        </p>
      )}
    </div>
  )
}
