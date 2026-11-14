import { useParams } from "react-router-dom"
import { LayoutTemplate } from "lucide-react"

import { usePageConfig } from "@/services/queries"
import { DwList } from "@/components/dw/DwList"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"

/**
 * 配置驱动的动态页面：/p/:pageKey → 拉配置 → 渲染。
 * <p>
 * 这是「配一个页面不用改代码」的落点：菜单配一条 /p/xxx + 页面配置一行，
 * 顶部导航就多一个入口。组件本身只是「配置 → DwList」的薄壳，复杂度都在
 * 配置结构与 DwList 渲染器里。
 * <p>
 * 权限说明：菜单可见性按菜单挂的权限点过滤，这里拉配置走数据源的读权限——
 * 两者通常是同一个点，但即使菜单配置漏了，配置请求也会被后端拦下，不会白屏渲染。
 */
export function DynamicPage() {
  const { pageKey = "" } = useParams<{ pageKey: string }>()
  const { data: config, isLoading, isError, error, refetch } = usePageConfig(pageKey)

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="space-y-2">
          <Skeleton className="h-9 w-56" />
          <Skeleton className="h-4 w-80" />
        </div>
        <Skeleton className="h-40 w-full" />
        <Skeleton className="h-72 w-full" />
      </div>
    )
  }

  if (isError || !config) {
    return (
      <div className="space-y-6">
        <div>
          <h2 className="text-3xl font-semibold tracking-tight">动态页面</h2>
          <p className="mt-2 text-sm text-muted-foreground">/p/{pageKey}</p>
        </div>
        <Card>
          <CardContent className="p-12 text-center">
            <LayoutTemplate className="mx-auto size-8 text-muted-foreground/40" />
            <p className="mt-3 text-sm font-medium">
              {error instanceof Error ? error.message : "页面配置加载失败"}
            </p>
            <p className="mt-1 text-xs text-muted-foreground">
              常见原因：配置已被删除，或当前账号没有对应数据源的查看权限。
            </p>
            <Button className="mt-4" variant="outline" onClick={() => refetch()}>
              重试
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <p className="text-sm text-muted-foreground">动态页面</p>
        <h2 className="mt-1 text-3xl font-semibold tracking-tight">{config.title}</h2>
        {config.description && <p className="mt-2 text-sm text-muted-foreground">{config.description}</p>}
      </div>

      <DwList pageKey={pageKey} config={config} />
    </div>
  )
}
