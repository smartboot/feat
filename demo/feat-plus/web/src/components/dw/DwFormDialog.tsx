import { useEffect, useState } from "react"

import type { DwFormConfig, DwRow } from "@/api/types"
import { DwFormFields, fieldText } from "@/components/dw/DwFormFields"
import { isFieldWritable } from "@/components/dw/meta"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"

/**
 * 通用动态表单弹窗：字段控件、必填、正则校验全部来自渲染配置。
 *
 * 校验从「底部一条红字」改成「错误挂在出错的那一行」：字段一多，用户不知道
 * 「请填写角色名称」指的是屏幕上哪一格；行内错误把提示和控件放在同一个视觉单元里，
 * 同时把焦点移到第一个出错的字段。
 *
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
  const [errors, setErrors] = useState<Record<number, string>>({})
  const isEdit = Boolean(row)

  useEffect(() => {
    if (!open) return
    const next: Record<string, string> = {}
    for (const field of form.fields) {
      next[field.key] = row ? fieldText(row[field.key]) : field.defaultValue ?? ""
    }
    setDraft(next)
    setErrors({})
  }, [open, row, form.fields])

  function update(key: string, value: string) {
    setDraft((current) => ({ ...current, [key]: value }))
  }

  async function submit() {
    const found: Record<number, string> = {}
    form.fields.forEach((field, index) => {
      // 只读字段（readonly / 编辑态的 createOnly）不参与校验：
      // 控件填不了却要它「必填」，用户只会卡在一个无法完成的弹窗里。
      if (!isFieldWritable(field, isEdit)) return
      const value = (draft[field.key] ?? "").trim()
      if (field.required && value === "") {
        found[index] = `请填写${field.label}`
        return
      }
      if (field.pattern && value !== "") {
        let valid = true
        try {
          valid = new RegExp(field.pattern).test(value)
        } catch {
          // 正则本身写坏了：配置期已拦过，这里放行，避免不让用户保存。
          valid = true
        }
        if (!valid) found[index] = field.patternError || `${field.label}的格式不正确`
      }
    })
    setErrors(found)
    const firstIndex = Object.keys(found)[0]
    if (firstIndex !== undefined) {
      document.getElementById(`dw-field-${firstIndex}`)?.scrollIntoView({ block: "center", behavior: "smooth" })
      return
    }

    const values: Record<string, string> = {}
    for (const field of form.fields) {
      // 不可写字段不提交：服务端会按 writeMode 回填旧值 / 丢弃，前端传过去只会制造「好像改了」的错觉。
      if (!isFieldWritable(field, isEdit)) continue
      values[field.key] = (draft[field.key] ?? "").trim()
    }
    await onSubmit(values)
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{isEdit ? form.editTitle : form.createTitle}</DialogTitle>
          <DialogDescription>{isEdit ? form.editDescription : form.createDescription}</DialogDescription>
        </DialogHeader>

        <div className="py-2">
          <DwFormFields
            form={form}
            values={draft}
            onChange={update}
            editing={isEdit}
            fieldErrors={errors}
          />
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
