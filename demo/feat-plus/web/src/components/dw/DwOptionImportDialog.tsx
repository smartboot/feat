import { useState } from "react"

import type { DwOption } from "@/api/types"
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
import { adminService } from "@/services/http-admin-service"

/**
 * 从站内接口拉一份字典，转成值映射。
 *
 * ## 为什么是「导入」而不是「引用接口」
 *
 * 列表单元格是同步渲染的（`DwCell` 是纯函数），一行一个请求既慢又会让首屏闪一下。
 * 所以列上的映射只能是静态的。但静态就意味着**会漂移**——权限点加一个，手抄的 13 条
 * 不会自己变成 14 条，表现是那一格突然冒出一串 `user:write`，而且不报错。
 *
 * 折中办法就是这里：静态存储 + 一键同步。配置者看到原始编码时，点进来重读一次即可；
 * 提交前的 `check_menu.py` 会拦住「种子数据和字典不一致」——两边各管一段。
 *
 * ## 为什么字段名要人填
 *
 * 权限点接口返回的是 `{ key, label }`，不是 `{ value, label }`。默认按 `value` 猜的话
 * 会得到一份全空的映射，界面显示「读到 13 行、映射 0 项」——比报错更难查。
 * 所以取值 / 显示字段必须显式填写，默认值只是占位提示。
 */
export function DwOptionImportDialog({
  open,
  onOpenChange,
  existing,
  onImported,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** 当前已有的映射，用于「追加」 */
  existing: DwOption[]
  /** 导入结果：替换或追加后的完整数组 */
  onImported: (options: DwOption[]) => void
}) {
  const [api, setApi] = useState("")
  const [valueField, setValueField] = useState("value")
  const [labelField, setLabelField] = useState("label")
  const [rows, setRows] = useState<Array<Record<string, unknown>> | null>(null)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState("")

  function reset() {
    setRows(null)
    setError("")
  }

  /** 接口原始行 → 映射项。取不到值的行直接丢掉：一个没有值的选项在任何控件里都没法选。 */
  function mapped(source: Array<Record<string, unknown>>): DwOption[] {
    return source
      .map((row) => {
        const value = String(row[valueField] ?? "").trim()
        const label = String(row[labelField] ?? "").trim()
        return { value, label: label || value, variant: "secondary" as const }
      })
      .filter((option) => option.value !== "")
  }

  async function read() {
    reset()
    const path = api.trim()
    // 与后端 PageService.requireApiPath 同一条规则：站内路径，防止把外部地址存进配置
    if (!path.startsWith("/api/")) {
      setError("只支持站内 /api/ 路径")
      return
    }
    setPending(true)
    try {
      const source = await adminService.fetchOptionRows(path)
      if (mapped(source).length === 0) {
        setError(`读到 ${source.length} 行，但按「${valueField}」取值后没有非空项——检查取值字段名`)
        return
      }
      setRows(source)
    } catch (e) {
      setError(e instanceof Error ? e.message : "读取失败")
    } finally {
      setPending(false)
    }
  }

  const preview = rows ? mapped(rows) : []
  const rawCount = rows?.length ?? 0

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        reset()
        onOpenChange(next)
      }}
    >
      <DialogContent className="sm:max-w-[460px]">
        <DialogHeader>
          <DialogTitle>从接口导入映射</DialogTitle>
          <DialogDescription>
            拉一次字典存成静态映射。列表渲染不发请求，所以这里是复制一份——接口那边新增的项不会
            自动出现，需要时再导入一次。
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-3">
          <div className="grid gap-1">
            <Label htmlFor="option-import-api">接口路径</Label>
            <Input
              id="option-import-api"
              value={api}
              placeholder="/api/permissions"
              onChange={(event) => {
                setApi(event.target.value)
                reset()
              }}
            />
          </div>
          <div className="grid grid-cols-2 gap-2">
            <div className="grid gap-1">
              <Label htmlFor="option-import-value">取值字段</Label>
              <Input
                id="option-import-value"
                value={valueField}
                placeholder="value"
                onChange={(event) => {
                  setValueField(event.target.value)
                  reset()
                }}
              />
            </div>
            <div className="grid gap-1">
              <Label htmlFor="option-import-label">显示字段</Label>
              <Input
                id="option-import-label"
                value={labelField}
                placeholder="label"
                onChange={(event) => {
                  setLabelField(event.target.value)
                  reset()
                }}
              />
            </div>
          </div>

          {error && <p className="text-xs text-destructive">{error}</p>}

          {rows && (
            <div className="rounded-md border bg-muted/30 p-2">
              <p className="text-xs">
                读到 {rawCount} 行，可用 {preview.length} 项
                {rawCount !== preview.length && "（有值字段为空的行已丢弃）"}
              </p>
              <ul className="mt-1 grid gap-0.5 text-[11px] text-muted-foreground">
                {preview.slice(0, 5).map((option) => (
                  <li key={option.value} className="truncate">
                    {option.value} → {option.label}
                  </li>
                ))}
                {preview.length > 5 && <li>…还有 {preview.length - 5} 项</li>}
              </ul>
            </div>
          )}
        </div>

        <DialogFooter>
          {rows && preview.length > 0 ? (
            <>
              <Button variant="outline" onClick={() => onImported([...existing, ...preview])}>
                追加 {preview.length} 项
              </Button>
              <Button onClick={() => onImported(preview)}>替换为 {preview.length} 项</Button>
            </>
          ) : (
            <Button onClick={read} disabled={pending || !api.trim()}>
              {pending ? "读取中…" : "读取"}
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
