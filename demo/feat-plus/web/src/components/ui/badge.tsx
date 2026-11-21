import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "@/lib/utils"

const badgeVariants = cva("inline-flex items-center gap-1 rounded-md border px-2 py-0.5 text-xs font-medium transition-colors", {
  variants: {
    variant: {
      default: "border-transparent bg-primary text-primary-foreground",
      secondary: "border-transparent bg-secondary text-secondary-foreground",
      destructive: "border-transparent bg-destructive text-white",
      outline: "text-foreground",
      /*
       * 状态徽章（成功/停用）：中性底 + 彩点，而不是饱和药丸。
       * 理由：状态是表格里信息价值最低的字段，不该比姓名还抢眼；
       * 彩点保留一瞥可辨的语义，底色回归中性后整表只有真正的
       * 主操作和危险操作带颜色。
       */
      success: "border-transparent bg-secondary text-secondary-foreground before:inline-block before:size-1.5 before:rounded-full before:bg-emerald-500 before:content-[''] dark:before:bg-emerald-400",
      warning: "border-transparent bg-secondary text-secondary-foreground before:inline-block before:size-1.5 before:rounded-full before:bg-amber-500 before:content-[''] dark:before:bg-amber-400",
    },
  },
  defaultVariants: { variant: "default" },
})

export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement>, VariantProps<typeof badgeVariants> {}
function Badge({ className, variant, ...props }: BadgeProps) { return <span className={cn(badgeVariants({ variant }), className)} {...props} /> }
export { Badge, badgeVariants }
