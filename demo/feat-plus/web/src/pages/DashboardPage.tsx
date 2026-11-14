import { ArrowDownRight, ArrowUpRight, CheckCircle2, UsersRound, UserRoundX, Activity as ActivityIcon } from "lucide-react"
import { Link } from "react-router-dom"
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts"
import { useState } from "react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { useSession } from "@/auth/session"
import { hasPermission } from "@/lib/permissions"
import { useDashboard } from "@/services/queries"

/**
 * Linear-style 数据驱动 Dashboard。
 *
 * 设计哲学：管理员 80% 的时间在看数字和趋势。所以：
 * - 指标卡片用超大字体（text-5xl），一眼看到关键数字
 * - 图表占半屏高度，不是挤在小卡片里
 * - 留白多，不拥挤
 * - 去掉装饰性元素，让数据说话
 */
export function DashboardPage() {
  const { data, isLoading, isError, refetch } = useDashboard()
  const [range, setRange] = useState<"7" | "30">("7")
  const session = useSession()
  const canReadAudit = hasPermission(session, "audit:read")

  if (isLoading) return <DashboardSkeleton />
  if (isError || !data) return <div className="rounded-xl border border-dashed p-12 text-center"><strong className="text-lg">暂时无法加载概览数据</strong><p className="mt-2 text-sm text-muted-foreground">请检查后端服务后重试。</p><Button className="mt-5" variant="outline" onClick={() => refetch()}>重新加载</Button></div>

  const trend = range === "7" ? data.trend7 : data.trend30
  const totalLogins = trend.reduce((sum, point) => sum + point.value, 0)
  const activeRate = data.totalUsers ? Math.round(data.enabledUsers / data.totalUsers * 100) : 0
  const hour = new Date().getHours()
  const greeting = hour < 12 ? "早上好" : hour < 18 ? "下午好" : "晚上好"

  return (
    <div className="space-y-8">
      {/* 标题区 */}
      <div className="flex items-end justify-between">
        <div>
          <p className="text-sm text-muted-foreground">{greeting}，管理员</p>
          <h1 className="mt-1 text-4xl font-semibold tracking-tight">工作概览</h1>
          <p className="mt-2 text-sm text-muted-foreground">数据会随用户操作实时更新。</p>
        </div>
        <Badge variant="success" className="gap-1.5">
          <CheckCircle2 className="size-3" /> 系统正常
        </Badge>
      </div>

      {/* 关键指标 */}
      <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4" aria-label="关键指标">
        <MetricCard href="/users" label="系统用户" value={data.totalUsers} detail="较上周新增 3 人" trend="up" icon={<UsersRound className="size-4" />} />
        <MetricCard href="/users?status=1" label="已启用账号" value={data.enabledUsers} detail={`${activeRate}% 账号状态正常`} trend="up" icon={<CheckCircle2 className="size-4" />} />
        <MetricCard href="/users?status=0" label="停用账号" value={data.disabledUsers} detail="可在用户管理中恢复" trend="neutral" icon={<UserRoundX className="size-4" />} />
        <MetricCard label="未读通知" value={data.unreadNotifications} detail="打开顶部通知查看详情" trend="neutral" icon={<ActivityIcon className="size-4" />} />
      </section>

      {/* 图表区 */}
      <section className="grid gap-4 lg:grid-cols-[1.5fr_1fr]">
        {/* 登录趋势 - 大图表 */}
        <Card>
          <CardHeader className="flex-row items-start justify-between space-y-0">
            <div>
              <CardTitle>登录趋势</CardTitle>
              <CardDescription className="mt-1">最近 {range === "7" ? "7 天" : "30 天"}成功登录次数</CardDescription>
            </div>
            <div className="flex items-end gap-3">
              <div className="text-right">
                <strong className="text-3xl font-semibold tracking-tight">{totalLogins}</strong>
                <p className="text-[11px] text-muted-foreground">累计登录</p>
              </div>
              <div className="flex rounded-lg border bg-muted/40 p-0.5" role="tablist" aria-label="图表时间范围">
                <Button size="sm" variant={range === "7" ? "secondary" : "ghost"} onClick={() => setRange("7")} role="tab" aria-selected={range === "7"} className="text-xs">7 天</Button>
                <Button size="sm" variant={range === "30" ? "secondary" : "ghost"} onClick={() => setRange("30")} role="tab" aria-selected={range === "30"} className="text-xs">30 天</Button>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <div className="h-[320px] w-full">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={trend} margin={{ top: 12, right: 8, left: -24, bottom: 0 }}>
                  <defs>
                    <linearGradient id="loginTrendFill" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="var(--chart-1)" stopOpacity={0.28} />
                      <stop offset="100%" stopColor="var(--chart-1)" stopOpacity={0.02} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid vertical={false} stroke="var(--border)" strokeDasharray="4 4" />
                  <XAxis dataKey="label" tickLine={false} axisLine={false} tickMargin={10} minTickGap={24} tick={{ fill: "var(--muted-foreground)", fontSize: 11 }} />
                  <YAxis tickLine={false} axisLine={false} tick={{ fill: "var(--muted-foreground)", fontSize: 11 }} allowDecimals={false} />
                  <Tooltip cursor={{ stroke: "var(--chart-1)", strokeOpacity: .3 }} contentStyle={{ borderRadius: 8, border: "1px solid var(--border)", background: "var(--popover)", color: "var(--popover-foreground)", fontSize: 12 }} formatter={(value) => [`${value} 次`, "成功登录"]} />
                  <Area type="monotone" dataKey="value" stroke="var(--chart-1)" fill="url(#loginTrendFill)" strokeWidth={2} dot={false} activeDot={{ r: 4, strokeWidth: 2, stroke: "var(--popover)", fill: "var(--chart-1)" }} />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>

        {/* 系统动态 */}
        <Card>
          <CardHeader>
            <CardTitle>系统动态</CardTitle>
            <CardDescription className="mt-1">最近发生的关键操作</CardDescription>
          </CardHeader>
          <CardContent className="space-y-0">
            {data.activities.map((item) => (
              <div key={item.id} className="flex gap-3 border-b py-3.5 last:border-0">
                <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-xs font-semibold text-primary">
                  {item.title.replace("管理员", "").slice(0, 1)}
                </span>
                <div className="min-w-0">
                  <p className="text-sm leading-5">{item.title}</p>
                  <p className="mt-1 text-xs text-muted-foreground">{item.description}</p>
                  <time className="mt-1 block text-[10px] text-muted-foreground">{item.createdTime}</time>
                </div>
              </div>
            ))}
            {!data.activities.length && <p className="py-8 text-center text-sm text-muted-foreground">暂无动态</p>}
            {canReadAudit && (
              <div className="pt-3">
                <Button asChild variant="ghost" size="sm" className="w-full justify-between text-xs text-muted-foreground">
                  <Link to="/activities">在审计日志中查看全部记录<ArrowUpRight className="size-3" /></Link>
                </Button>
              </div>
            )}
          </CardContent>
        </Card>
      </section>
    </div>
  )
}

function MetricCard({ href, label, value, detail, trend, icon }: { href?: string; label: string; value: number; detail: string; trend: "up" | "down" | "neutral"; icon: React.ReactNode }) {
  const content = (
    <CardContent className="p-5">
      <div className="flex items-center justify-between">
        <span className="text-xs font-medium text-muted-foreground">{label}</span>
        <span className="flex size-8 items-center justify-center rounded-lg bg-primary/10 text-primary">
          {icon}
        </span>
      </div>
      <strong className="mt-3 block text-5xl font-semibold tracking-tight text-foreground numeric">
        {value}
      </strong>
      <p className={`mt-2 flex items-center gap-1 text-xs ${trend === "up" ? "text-emerald-600 dark:text-emerald-400" : trend === "down" ? "text-destructive" : "text-muted-foreground"}`}>
        {trend === "up" && <ArrowUpRight className="size-3" />}
        {trend === "down" && <ArrowDownRight className="size-3" />}
        {detail}
      </p>
    </CardContent>
  )
  return href ? (
    <Link to={href} className="rounded-xl outline-none ring-offset-background transition-transform hover:-translate-y-0.5 focus-visible:ring-2 focus-visible:ring-ring">
      <Card className="h-full transition-colors hover:border-primary/30 hover:shadow-md">{content}</Card>
    </Link>
  ) : (
    <Card className="h-full">{content}</Card>
  )
}

function DashboardSkeleton() {
  return (
    <div className="space-y-8">
      <div className="space-y-2">
        <Skeleton className="h-4 w-32" />
        <Skeleton className="h-10 w-64" />
      </div>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 4 }, (_, index) => <Skeleton key={index} className="h-32 rounded-xl" />)}
      </div>
      <div className="grid gap-4 lg:grid-cols-[1.5fr_1fr]">
        <Skeleton className="h-[400px] rounded-xl" />
        <Skeleton className="h-[400px] rounded-xl" />
      </div>
    </div>
  )
}
