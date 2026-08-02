import { useState, type FormEvent } from "react"
import { ArrowRight, CheckCircle2, Eye, EyeOff, KeyRound, Loader2, ShieldCheck, Sparkles } from "lucide-react"
import { useLocation } from "react-router-dom"

import type { CurrentUser } from "@/api/types"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { adminService } from "@/services/mock-admin-service"

interface LoginPageProps { onSuccess: (token: string, user: CurrentUser) => void }

export function LoginPage({ onSuccess }: LoginPageProps) {
  const [username, setUsername] = useState("admin")
  const [password, setPassword] = useState("admin123")
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")
  const location = useLocation()

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

  return <main className="grid min-h-screen lg:grid-cols-[1.1fr_.9fr]">
    <section className="relative hidden overflow-hidden bg-slate-950 px-10 py-10 text-white lg:flex lg:flex-col xl:px-20">
      <div className="absolute -right-32 top-20 size-96 rounded-full bg-blue-500/20 blur-3xl" /><div className="absolute -bottom-40 left-1/4 size-96 rounded-full bg-cyan-400/10 blur-3xl" />
      <div className="relative flex items-center gap-3"><img src="/feat-logo.svg" alt="Feat Plus" className="size-9 rounded-xl" /><span className="text-lg font-semibold tracking-tight">Feat Plus</span></div>
      <div className="relative my-auto max-w-xl"><div className="mb-6 inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-3 py-1.5 text-xs text-slate-300"><Sparkles className="size-3.5 text-blue-300" />企业管理系统开发底座</div><h1 className="text-5xl font-semibold leading-[1.08] tracking-[-.05em] xl:text-6xl">简单，但不简陋。<br /><span className="text-blue-300">让管理系统回归业务。</span></h1><p className="mt-6 max-w-lg text-base leading-7 text-slate-400">基于 Feat Cloud 与 shadcn/ui，为团队提供清晰、轻量、易扩展的企业应用起点。</p><ul className="mt-9 grid gap-4 text-sm text-slate-300"><li className="flex items-center gap-3"><CheckCircle2 className="size-4 text-emerald-400" />清晰的业务边界与可演进架构</li><li className="flex items-center gap-3"><CheckCircle2 className="size-4 text-emerald-400" />现代化管理界面与一致的交互规范</li><li className="flex items-center gap-3"><CheckCircle2 className="size-4 text-emerald-400" />认证、权限与审计能力的演示起点</li></ul></div>
      <p className="relative text-xs text-slate-500">Feat Plus · Built for focused teams</p>
    </section>
    <section className="flex min-h-screen items-center justify-center bg-background p-6 sm:p-10"><Card className="w-full max-w-sm border-0 shadow-none"><CardHeader className="space-y-4 px-0"><div className="flex size-10 items-center justify-center rounded-xl bg-primary/10 text-primary"><ShieldCheck className="size-5" /></div><div><CardTitle className="text-2xl tracking-tight">欢迎回来</CardTitle><CardDescription className="mt-2">登录你的 Feat Plus 工作空间</CardDescription></div></CardHeader><CardContent className="px-0"><form className="grid gap-5" onSubmit={handleSubmit} noValidate><div className="grid gap-2"><Label htmlFor="username">用户名</Label><Input id="username" autoComplete="username" value={username} onChange={(event) => setUsername(event.target.value)} aria-invalid={Boolean(error)} /></div><div className="grid gap-2"><div className="flex items-center justify-between"><Label htmlFor="password">密码</Label><span className="text-xs text-muted-foreground">演示环境</span></div><div className="relative"><KeyRound className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" /><Input className="pl-9 pr-10" id="password" type={showPassword ? "text" : "password"} autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} aria-invalid={Boolean(error)} aria-describedby={error ? "login-error" : undefined} /><Button type="button" variant="ghost" size="icon" className="absolute right-0 top-0" onClick={() => setShowPassword((value) => !value)} aria-label={showPassword ? "隐藏密码" : "显示密码"}>{showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}</Button></div></div>{error && <p id="login-error" role="alert" className="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">{error}</p>}<Button className="w-full" type="submit" disabled={loading}>{loading ? <><Loader2 className="size-4 animate-spin" />正在登录</> : <>进入系统<ArrowRight className="size-4" /></>}</Button></form><div className="mt-6 rounded-lg border bg-muted/40 px-3 py-2.5 text-center text-xs text-muted-foreground">演示账号 <code className="font-medium text-foreground">admin</code><span className="mx-1">/</span><code className="font-medium text-foreground">admin123</code></div><p className="mt-8 text-center text-[11px] text-muted-foreground">{location.pathname === "/login" ? "继续即表示你已了解本项目目前用于演示与开发验证" : "正在返回你的工作空间"}</p></CardContent></Card></section>
  </main>
}
