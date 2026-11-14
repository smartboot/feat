import { useEffect, useState } from "react"

import type { DwFormConfig, DwFormField, DwRow } from "@/api/types"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Switch } from "@/components/ui/switch"
import { Textarea } from "@/components/ui/textarea"
import { cn } from "@/lib/utils"

/**
 * 通用动态表单弹窗：字段控件、必填、正则校验全部来自渲染配置。
 * 校验风格与手写页保持一致——单条错误文案，不做逐字段错误矩阵：
 * 动态表单的字段顺序由配置决定，「第一条错误」就是用户下一个要看的位置。
 * <p>
 * 值统一字符串（switch 用 "1"/"0"），与后端 DwSaveRequest 契约对齐。
 */
export function DwFormDialog({
  open,
  onOpenChange,
  form,
  row,
  onSubmit,
  pending,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  form: DwFormConfig
  /** 编辑中的行数据；null 表示新建。 */
  row: DwRow | null
  onSubmit: (values: Record<string, string>) => Promise<void>
  pending: boolean
}) {
  const [draft, setDraft] = useState<Record<string, string>>({})
  const [error, setError] = useState("")
  const isEdit = Boolean(row)

  useEffect(() => {
    if (!open) return
    const next: Record<string, string> = {}
    for (const field of form.fields) {
      next[field.key] = row ? fieldText(row[field.key]) : field.defaultValue ?? ""
    }
    setDraft(next)
    setError("")
  }, [open, row, form.fields])

  function update(key: string, value: string) {
    setDraft((current) => ({ ...current, [key]: value }))
  }

  async function submit() {
    for (const field of form.fields) {
      const value = (draft[field.key] ?? "").trim()
      if (field.required && value === "") {
        setError(`请填写${field.label}`)
        return
      }
      if (field.pattern && value !== "" && !new RegExp(field.pattern).test(value)) {
        setError(field.patternError || `${field.label}的格式不正确`)
        return
      }
    }
    setError("")
    const values: Record<string, string> = {}
    for (const field of form.fields) {
      values[field.key] = (draft[field.key] ?? "").trim()
    }
    await onSubmit(values)
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{isEdit ? form.editTitle : form.createTitle}</DialogTitle>
          <DialogDescription>
            {isEdit ? form.editDescription : form.createDescription}
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-4 py-2">
          <div className="grid gap-4 sm:grid-cols-2">
            {form.fields.map((field) => (
              <div
                key={field.key}
                className={cn("grid gap-2", (field.span ?? 1) === 2 && "sm:col-span-2")}
              >
                {field.widget !== "switch" && (
                  <Label htmlFor={`dw-field-${field.key}`}>
                    {field.label}
                    {field.required && <span className="text-destructive"> *</span>}
                  </Label>
                )}
                {renderField(field, draft[field.key] ?? "", update, Boolean(row))}
              </div>
            ))}
          </div>

          {error && <p className="text-sm text-destructive" role="alert">{error}</p>}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            取消
          </Button>
          <Button onClick={submit} disabled={pending}>
            {pending ? "正在保存..." : isEdit ? "保存修改" : form.createTitle}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function renderField(
  field: DwFormField,
  value: string,
  update: (key: string, value: string) => void,
  editing: boolean,
) {
  const id = `dw-field-${field.key}`
  // immutable = 创建可填、编辑只读（角色编码这类锚点字段）。
  const locked = Boolean(field.immutable && editing)
  switch (field.widget) {
    case "password":
      return <Input id={id} type="password" value={value} disabled={locked} placeholder={field.placeholder} onChange={(event) => update(field.key, event.target.value)} />
    case "textarea":
      return <Textarea id={id} value={value} disabled={locked} placeholder={field.placeholder} onChange={(event) => update(field.key, event.target.value)} />
    case "select":
      return (
        <Select value={value} onValueChange={(next) => update(field.key, next)} disabled={locked}>
          <SelectTrigger id={id}>
            <SelectValue placeholder={field.placeholder || "请选择"} />
          </SelectTrigger>
          <SelectContent>
            {(field.options ?? []).map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      )
    case "switch":
      return (
        <div className="flex items-center justify-between rounded-lg border p-3">
          <div>
            <Label htmlFor={id}>{field.label}</Label>
          </div>
          <Switch id={id} checked={value === "1"} disabled={locked} onCheckedChange={(checked) => update(field.key, checked ? "1" : "0")} />
        </div>
      )
    case "span":
      return <p className="min-h-9 rounded-md border border-input bg-muted/40 px-3 py-2 text-sm">{value || "—"}</p>
    case "input":
    default:
      return <Input id={id} value={value} disabled={locked} placeholder={field.placeholder} onChange={(event) => update(field.key, event.target.value)} />
  }
}

function fieldText(value: unknown): string {
  if (value == null) return ""
  return String(value)
}
