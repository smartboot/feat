import { useState, type FormEvent } from "react"
import { ArrowRight, CheckCircle2, Eye, EyeOff, KeyRound, Loader2, ShieldCheck, Sparkles } from "lucide-react"

import type { CurrentUser } from "@/api/types"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { adminService } from "@/services/http-admin-service"

interface LoginPageProps { onSuccess: (token: string, user: CurrentUser) => void }

export function LoginPage({ onSuccess }: LoginPageProps) {
  const [username, setUsername] = useState("admin")
  const [password, setPassword] = useState("admin123")
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!username.trim() || !password) { setError("请输入用户名和密码"); return }
    setLoading(true)
    setError("")
    try {
      const response = await adminService.login(username.trim(), password)
      onSuccess(response.accessToken, response.user)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "登录失败")
    } finally { setLoading(false) }
  }

  return (
    <main className="grid min-h-screen lg:grid-cols-[1.1fr_.9fr]">
      {/* 左侧品牌区 */}
      <section className="relative hidden overflow-hidden bg-gradient-to-br from-background via-background to-primary/5 px-10 py-10 text-foreground lg:flex lg:flex-col xl:px-20">
        <div className="absolute -right-40 top-20 size-[500px] rounded-full bg-primary/10 blur-[120px]" />
        <div className="absolute -bottom-48 left-1/4 size-[420px] rounded-full bg-chart-2/10 blur-[120px]" />
        <div className="absolute right-1/3 top-1/2 size-64 rounded-full bg-chart-3/5 blur-[100px]" />
        <div className="relative flex items-center gap-3">
          <img src="/feat-logo.svg" alt="Feat Plus" className="size-9 rounded-xl" />
          <span className="text-lg font-semibold tracking-tight">Feat Plus</span>
        </div>
        <div className="relative my-auto max-w-xl">
          <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-border bg-card/50 px-3 py-1.5 text-xs backdrop-blur-sm">
            <Sparkles className="size-3.5 text-primary" />
            企业管理系统开发底座
          </div>
          <h1 className="text-5xl font-semibold leading-[1.08] tracking-[-.04em] xl:text-6xl">
            简单，但不简陋。<br />
            <span className="bg-gradient-to-r from-primary via-chart-2 to-primary bg-clip-text text-transparent">
              让管理系统回归业务。
            </span>
          </h1>
          <p className="mt-6 max-w-lg text-base leading-7 text-muted-foreground">
            基于 Feat Cloud 与 shadcn/ui，为团队提供清晰、轻量、易扩展的企业应用起点。
          </p>
          <ul className="mt-9 grid gap-4 text-sm text-muted-foreground">
            <li className="flex items-center gap-3">
              <CheckCircle2 className="size-4 text-emerald-500" />
              清晰的业务边界与可演进架构
            </li>
            <li className="flex items-center gap-3">
              <CheckCircle2 className="size-4 text-emerald-500" />
              现代化管理界面与一致的交互规范
            </li>
            <li className="flex items-center gap-3">
              <CheckCircle2 className="size-4 text-emerald-500" />
              认证、权限与审计能力的演示起点
            </li>
          </ul>
        </div>
        <p className="relative text-xs text-muted-foreground">Feat Plus · Built for focused teams</p>
      </section>

      {/* 右侧登录表单 */}
      <section className="relative flex min-h-screen items-center justify-center bg-background p-6 sm:p-10">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top_right,_var(--color-primary)_/_3%,_transparent_60%)] opacity-40" />
        <Card className="relative w-full max-w-sm border shadow-[0_20px_60px_-15px_rgb(0_0_0_/_0.4)] dark:border-border/80">
          <CardHeader className="space-y-4 px-8 pt-8">
            <div className="flex size-11 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <ShieldCheck className="size-5" />
            </div>
            <div>
              <CardTitle className="text-2xl tracking-tight">欢迎回来</CardTitle>
              <CardDescription className="mt-2">登录你的 Feat Plus 工作空间</CardDescription>
            </div>
          </CardHeader>
          <CardContent className="px-8 pb-8">
            <form className="grid gap-5" onSubmit={handleSubmit} noValidate>
              <div className="grid gap-2">
                <Label htmlFor="username">用户名</Label>
                <Input id="username" autoComplete="username" value={username} onChange={(event) => setUsername(event.target.value)} aria-invalid={Boolean(error)} />
              </div>
              <div className="grid gap-2">
                <div className="flex items-center justify-between">
                  <Label htmlFor="password">密码</Label>
                  <span className="text-xs text-muted-foreground">演示环境</span>
                </div>
                <div className="relative">
                  <KeyRound className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                  <Input className="pl-9 pr-10" id="password" type={showPassword ? "text" : "password"} autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} aria-invalid={Boolean(error)} aria-describedby={error ? "login-error" : undefined} />
                  <Button type="button" variant="ghost" size="icon" className="absolute right-0 top-0" onClick={() => setShowPassword((value) => !value)} aria-label={showPassword ? "隐藏密码" : "显示密码"}>
                    {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
                  </Button>
                </div>
              </div>
              {error && <p id="login-error" role="alert" className="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">{error}</p>}
              <Button className="w-full" type="submit" disabled={loading}>
                {loading ? <><Loader2 className="size-4 animate-spin" />正在登录</> : <>进入系统<ArrowRight className="size-4" /></>}
              </Button>
            </form>
            <div className="mt-6 rounded-lg border bg-muted/40 px-3 py-2.5 text-center text-xs text-muted-foreground">
              演示账号 <code className="font-medium text-foreground">admin</code>
              <span className="mx-1">/</span>
              <code className="font-medium text-foreground">admin123</code>
            </div>
            <p className="mt-8 text-center text-[11px] text-muted-foreground">
              {location.pathname === "/login" ? "继续即表示你已了解本项目目前用于演示与开发验证" : "正在返回你的工作空间"}
            </p>
          </CardContent>
        </Card>
      </section>
    </main>
  )
}
