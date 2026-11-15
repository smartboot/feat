import type { DragEvent, ReactNode } from "react"
import { ChevronDown, ChevronUp, GripVertical, Trash2 } from "lucide-react"

import { Button } from "@/components/ui/button"
import { MarkDot } from "@/components/dw/DwEditorBits"
import { focusAnchor, sameFocus, type DwFocusTarget, type EditorLink } from "@/components/dw/link"
import { cn } from "@/lib/utils"

/**
 * 列 / 筛选 / 自定义按钮的「表格」骨架。
 *
 * 这三样天然是表格：一行一条，行与行共用同一套栅格，才能横向比较——
 * 「这一列比那一列宽」「顺序对不对」「哪个字段被筛了两次」。卡片堆叠里做不了这件事：
 * 每张卡片各自展开，宽度档位和相对顺序都看不见，10 列就要滚三屏。
 *
 * 这里只负责三件事：栅格对齐、行内联动高亮、顺序调整。
 * 每一格渲染什么由调用方给，因为列 / 筛选 / 自定义按钮的字段不一样。
 */
export const COLUMN_TEMPLATE = "16px minmax(0,1.45fr) minmax(0,1.05fr) 58px 94px 80px"
export const FILTER_TEMPLATE = "16px minmax(0,1.3fr) minmax(0,0.95fr) 88px minmax(0,1.1fr) 80px"
export const ACTION_TEMPLATE = "16px minmax(0,1fr) minmax(0,1.5fr) minmax(0,1.1fr) 76px"

/** 拖动一行所需的回调。手柄可拖，行负责接收落点。 */
export interface RowDrag {
  dragging: boolean
  onDragStart: (event: DragEvent<HTMLSpanElement>) => void
  onDragOver: (event: DragEvent<HTMLDivElement>) => void
  onDrop: (event: DragEvent<HTMLDivElement>) => void
  onDragEnd: () => void
}

/** 把「拖到第几条」翻译成新数组。上下移与拖拽共用，顺序变更只有这一个出口。 */
export function reorder<T>(list: T[], from: number, to: number): T[] {
  if (from === to || from < 0 || to < 0 || from >= list.length || to >= list.length) return list
  const next = [...list]
  const [moved] = next.splice(from, 1)
  next.splice(to, 0, moved)
  return next
}

export function RowTable({
  template,
  header,
  children,
  footer,
}: {
  template: string
  header: ReactNode[]
  children: ReactNode
  footer?: ReactNode
}) {
  return (
    <div className="overflow-hidden rounded-lg border">
      <div
        className="grid items-center gap-2 border-b bg-muted/40 px-2 py-1.5 text-[11px] text-muted-foreground"
        style={{ gridTemplateColumns: template }}
      >
        {header.map((cell, index) => (
          <span key={index} className="truncate">
            {cell}
          </span>
        ))}
      </div>
      <div className="divide-y">{children}</div>
      {footer}
    </div>
  )
}

/**
 * 一行。`children` 是主栅格里的各格（数量由 template 决定），`detail` 是展开后的二级配置。
 *
 * 展开态挂在调用方（`expanded` + `onToggleExpand`）而不是内部 state：
 * 同一时刻只该展开一条，两条同时展开就又回到「卡片堆」那种看不完的状态。
 */
export function RowTableItem({
  template,
  target,
  link,
  index,
  total,
  onMove,
  onRemove,
  expandable = true,
  expanded = false,
  onToggleExpand,
  drag,
  detail,
  detailClassName,
  children,
}: {
  template: string
  /** 联动目标。预览里没有对应元素的条目（如自定义按钮）不传，这一行就不参与高亮。 */
  target?: DwFocusTarget
  link?: EditorLink
  index: number
  total: number
  onMove?: (delta: number) => void
  /** 行尾的垃圾桶。没有二级配置区的条目靠它删除（列 / 筛选的删除在展开区里） */
  onRemove?: () => void
  /** 没有二级配置的行（自定义按钮）不给展开入口 */
  expandable?: boolean
  expanded?: boolean
  onToggleExpand?: () => void
  drag?: RowDrag
  detail?: ReactNode
  detailClassName?: string
  children: ReactNode
}) {
  const focused = target ? sameFocus(link?.focus, target) : false
  const mark = target ? link?.marks?.[target.kind][target.index] : undefined

  return (
    <>
      <div
        id={target ? focusAnchor(target) : undefined}
        data-link-focus={focused && target ? focusAnchor(target) : undefined}
        className={cn(
          "grid items-center gap-2 px-2 py-1.5 transition-colors",
          focused ? "bg-primary/5 ring-1 ring-inset ring-primary/30" : "hover:bg-muted/40",
          drag?.dragging && "opacity-40",
        )}
        style={{ gridTemplateColumns: template }}
        onMouseEnter={() => target && link?.onHover?.(target)}
        onMouseLeave={() => target && link?.onHover?.(null)}
        onFocusCapture={() => target && link?.onSelect?.(target)}
        onDragOver={drag?.onDragOver}
        onDrop={drag?.onDrop}
      >
        <span
          draggable={Boolean(drag)}
          onDragStart={drag?.onDragStart}
          onDragEnd={drag?.onDragEnd}
          className={cn(
            "flex items-center justify-center text-muted-foreground/40",
            drag && "cursor-grab hover:text-muted-foreground active:cursor-grabbing",
          )}
          aria-hidden={drag ? undefined : true}
        >
          {drag && <GripVertical className="size-3" />}
        </span>

        {children}

        <span className="flex items-center justify-end gap-0.5">
          {mark && <MarkDot level={mark} />}
          {onMove && (
            <>
              <Button
                variant="ghost"
                size="icon"
                className="size-6 text-muted-foreground/60 hover:text-foreground"
                aria-label="上移"
                disabled={index === 0}
                onClick={() => onMove(-1)}
              >
                <ChevronUp className="size-3" />
              </Button>
              <Button
                variant="ghost"
                size="icon"
                className="size-6 text-muted-foreground/60 hover:text-foreground"
                aria-label="下移"
                disabled={index === total - 1}
                onClick={() => onMove(1)}
              >
                <ChevronDown className="size-3" />
              </Button>
            </>
          )}
          {expandable && (
            <Button
              variant="ghost"
              size="icon"
              className={cn("size-6 text-muted-foreground/60 hover:text-foreground", expanded && "text-primary")}
              aria-label={expanded ? "收起配置" : "展开配置"}
              aria-expanded={expanded}
              onClick={onToggleExpand}
            >
              <ChevronDown className={cn("size-3.5 transition-transform", expanded && "rotate-180")} />
            </Button>
          )}
          {onRemove && (
            <Button
              variant="ghost"
              size="icon"
              className="size-6 text-muted-foreground/60 hover:text-destructive"
              aria-label="删除"
              onClick={onRemove}
            >
              <Trash2 className="size-3" />
            </Button>
          )}
        </span>
      </div>
      {expanded && detail && (
        <div className={cn("bg-muted/20 px-3 py-2.5", detailClassName)}>{detail}</div>
      )}
    </>
  )
}

/**
 * 展开区底部的一行：左边是这一条的附加说明，右边是删除。
 *
 * 删除不放主栅格里——一行只有 80px 的余量，误点一次就是丢一列。
 * 按钮的 aria-label 带上这一条的名字：读屏要能听出「删的是哪一条」，
 * 测试里也要能和动作区的「删除」按钮区分开。
 */
export function RowDelete({ name, label, onRemove }: { name: string; label: string; onRemove: () => void }) {
  return (
    <div className="mt-1 flex items-center justify-between gap-2">
      <span className="text-[11px] text-muted-foreground">{label}</span>
      <Button
        variant="ghost"
        size="sm"
        className="h-6 gap-1 px-1.5 text-[11px] text-destructive"
        aria-label={`删除「${name}」`}
        onClick={onRemove}
      >
        删除
      </Button>
    </div>
  )
}
