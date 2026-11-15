import type { ReactNode } from "react"
import { ChevronDown, ChevronUp, Plus, Trash2, Wand2 } from "lucide-react"

import { focusAnchor, sameFocus, type DwFocusTarget, type DwMarkLevel, type EditorLink } from "@/components/dw/link"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { cn } from "@/lib/utils"

/**
 * 编辑区共用骨架：分区标题 + 条目卡片。
 *
 * 列表视图与表单两个编辑器原本各写一套行布局，结果是「列」有上移下移、「字段」的
 * 间距和边框又略有不同。联动要成立，两边的条目必须是同一个东西——所以卡片在这里，
 * 高亮也在这里：左边的 ring、右边预览的高亮、问题点，全部由同一个 task 的 focus 驱动。
 */
export function ItemRow({
  target,
  link,
  title,
  subtitle,
  index,
  total,
  onMove,
  onRemove,
  children,
}: {
  /** 联动目标。预览里没有对应元素的条目（如自定义行操作）不传，卡片就不参与高亮与滚动。 */
  target?: DwFocusTarget
  link?: EditorLink
  title: string
  subtitle: string
  index: number
  total: number
  onMove: (delta: number) => void
  onRemove: () => void
  children: ReactNode
}) {
  const focused = target ? sameFocus(link?.focus, target) : false
  const mark = target ? link?.marks?.[target.kind][target.index] : undefined

  return (
    <div
      id={target ? focusAnchor(target) : undefined}
      data-link-focus={focused && target ? focusAnchor(target) : undefined}
      className={cn(
        "grid gap-2 rounded-lg border bg-card/40 p-2.5 transition-shadow",
        focused && "border-primary/50 ring-2 ring-primary/20",
      )}
      // 联动只认「指针进入」和「内部控件获得焦点」：点击输入框本身就等于选中这一条，
      // 不需要额外的整行点击热区去抢输入框的焦点。
      onMouseEnter={() => target && link?.onHover?.(target)}
      onMouseLeave={() => link?.onHover?.(null)}
      onFocusCapture={() => target && link?.onSelect?.(target)}
    >
      <div className="flex items-center justify-between gap-2">
        <div className="min-w-0">
          <p className="flex items-center gap-1.5 truncate text-xs font-medium">
            <span className="truncate">{title}</span>
            {mark && <MarkDot level={mark} />}
          </p>
          <p className="truncate text-[10px] text-muted-foreground">{subtitle}</p>
        </div>
        <div className="flex shrink-0 items-center gap-0.5">
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

export function Section({
  title,
  anchor,
  count,
  empty,
  onGenerate,
  onAdd,
  addDisabled = false,
  footer,
  children,
}: {
  title: string
  anchor: string
  count: number
  empty: string
  onGenerate?: () => void
  onAdd: () => void
  /** 到上限 / 只读源：按钮灰掉，而不是点了没反应 */
  addDisabled?: boolean
  footer?: string
  children: ReactNode
}) {
  return (
    <div className="grid gap-2" id={anchor}>
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
          <Button variant="outline" size="sm" className="h-7 gap-1 text-xs" onClick={onAdd} disabled={addDisabled}>
            <Plus className="size-3" />
            添加
          </Button>
        </div>
      </div>
      {count === 0 ? (
        <p className="rounded-md border border-dashed p-3 text-xs text-muted-foreground">{empty}</p>
      ) : (
        <div className="grid gap-2">{children}</div>
      )}
      {footer && <p className="text-[11px] text-muted-foreground">{footer}</p>}
    </div>
  )
}

/**
 * 问题标记点。编辑区与预览共用：同一处配置在两边标同一个颜色，
 * 「右边红了」和「左边红了」才指向同一个原因。
 */
export function MarkDot({ level }: { level: DwMarkLevel }) {
  return (
    <span
      role="img"
      aria-label={level === "error" ? "配置错误" : "配置提示"}
      title={level === "error" ? "这里有配置错误，保存会被拒绝" : "这里有一条提示，可以保存"}
      className={cn("inline-block size-1.5 shrink-0 rounded-full", level === "error" ? "bg-destructive" : "bg-amber-500")}
    />
  )
}

/** 行内小标签：视觉层级低于表单主 Label。 */
export function FieldLabel({ children }: { children: ReactNode }) {
  return <span className="text-[11px] text-muted-foreground">{children}</span>
}

/** 解释性文案：一句话说清这个配置项会发生什么，不写「请填写 XXX」。 */
export function Hint({ children }: { children: ReactNode }) {
  return <p className="text-[11px] leading-relaxed text-muted-foreground">{children}</p>
}
