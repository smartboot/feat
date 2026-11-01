import { useEffect, useState } from "react"
import { KeyRound, Save, UserRound } from "lucide-react"
import { toast } from "sonner"

import type { Profile } from "@/api/types"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Separator } from "@/components/ui/separator"
import { Skeleton } from "@/components/ui/skeleton"
import { useProfile, useProfileMutations } from "@/services/queries"

/** 与后端 ProfileService.MIN_PASSWORD_LENGTH 保持一致；改一处必须同时改另一处。 */
const MIN_PASSWORD_LENGTH = 8

/**
 * 个人中心。
 * <p>
 * 这个页面对所有登录用户可见，不需要任何权限点——改自己的昵称和密码是基本权利，
 * 不是管理动作。部门、角色、数据范围在这里一律只读：它们由管理员分配，
 * 允许本人改等于把权限系统开了一个后门。
 */
export function ProfilePage({ onProfileUpdated }: { onProfileUpdated?: () => void }) {
  const { data: profile, isLoading, isError, refetch } = useProfile()

  if (isLoading) {
    return (
      <div className="space-y-6">
        <ProfileHeader />
        <Skeleton className="h-64 w-full" />
        <Skeleton className="h-56 w-full" />
      </div>
    )
  }

  if (isError || !profile) {
    return (
      <div className="space-y-6">
        <ProfileHeader />
        <Card>
          <CardContent className="flex flex-col items-start gap-3 py-10">
            <p className="text-sm text-muted-foreground">资料加载失败，请重试。</p>
            <Button variant="outline" size="sm" onClick={() => refetch()}>重新加载</Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <ProfileHeader />
      <ProfileForm profile={profile} onProfileUpdated={onProfileUpdated} />
      <PasswordForm />
    </div>
  )
}

function ProfileHeader() {
  return (
    <div>
      <p className="text-sm text-muted-foreground">工作台</p>
      <h2 className="mt-1 text-3xl font-semibold tracking-tight">个人中心</h2>
      <p className="mt-2 text-sm text-muted-foreground">
        修改自己的昵称、邮箱与密码。部门与角色由管理员分配，这里只做展示。
      </p>
    </div>
  )
}

function ProfileForm({ profile, onProfileUpdated }: { profile: Profile; onProfileUpdated?: () => void }) {
  const mutations = useProfileMutations()
  const [displayName, setDisplayName] = useState(profile.displayName)
  const [email, setEmail] = useState(profile.email ?? "")
  const [error, setError] = useState("")

  // 资料被其它操作刷新时同步表单，否则会显示上一版内容。
  useEffect(() => {
    setDisplayName(profile.displayName)
    setEmail(profile.email ?? "")
  }, [profile.displayName, profile.email])

  const dirty = displayName !== profile.displayName || email !== (profile.email ?? "")

  async function submit() {
    if (!displayName.trim()) {
      setError("昵称不能为空")
      return
    }
    setError("")
    try {
      await mutations.update.mutateAsync({ displayName: displayName.trim(), email: email.trim() })
      toast.success("资料已更新")
      // 侧边栏头像旁的名字来自 /api/auth/me，不通知外层刷新就会一直是旧值。
      onProfileUpdated?.()
    } catch (e) {
      setError(e instanceof Error ? e.message : "保存失败")
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <UserRound className="size-4" /> 基本信息
        </CardTitle>
        <CardDescription>用户名与所属部门由管理员维护，如需变更请联系管理员。</CardDescription>
      </CardHeader>
      <CardContent className="space-y-5">
        <div className="grid gap-4 sm:grid-cols-2">
          <ReadOnlyField label="用户名" value={profile.username} />
          <ReadOnlyField label="所属部门" value={profile.departmentName || "未分配"} />
          <div className="grid gap-1.5">
            <span className="text-xs text-muted-foreground">角色</span>
            <div className="flex min-h-6 flex-wrap gap-1">
              {profile.roleNames.length === 0
                ? <span className="text-sm text-muted-foreground">未分配</span>
                : profile.roleNames.map((name) => <Badge key={name} variant="secondary">{name}</Badge>)}
            </div>
          </div>
          <ReadOnlyField label="数据范围" value={profile.scopeDescription} hint="决定你在列表里能看到的部门范围" />
          <ReadOnlyField label="创建时间" value={profile.createdTime || "—"} />
        </div>

        <Separator />

        <div className="grid gap-4 sm:grid-cols-2">
          <div className="grid gap-1.5">
            <Label htmlFor="profile-name">昵称 *</Label>
            <Input
              id="profile-name"
              value={displayName}
              maxLength={32}
              onChange={(event) => setDisplayName(event.target.value)}
              placeholder="显示在界面上的名字"
            />
          </div>
          <div className="grid gap-1.5">
            <Label htmlFor="profile-email">邮箱</Label>
            <Input
              id="profile-email"
              value={email}
              type="email"
              maxLength={128}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="name@example.com"
            />
          </div>
        </div>

        {error && <p className="text-sm text-destructive">{error}</p>}

        <div className="flex items-center gap-3">
          <Button onClick={submit} disabled={!dirty || mutations.update.isPending}>
            <Save className="size-4" /> {mutations.update.isPending ? "保存中..." : "保存修改"}
          </Button>
          {!dirty && <span className="text-xs text-muted-foreground">没有待保存的改动</span>}
        </div>
      </CardContent>
    </Card>
  )
}

function PasswordForm() {
  const mutations = useProfileMutations()
  const [oldPassword, setOldPassword] = useState("")
  const [newPassword, setNewPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [error, setError] = useState("")

  async function submit() {
    if (!oldPassword) {
      setError("请输入当前密码")
      return
    }
    if (newPassword.length < MIN_PASSWORD_LENGTH) {
      setError(`新密码至少 ${MIN_PASSWORD_LENGTH} 位`)
      return
    }
    if (newPassword !== confirmPassword) {
      setError("两次输入的新密码不一致")
      return
    }
    setError("")
    try {
      const revoked = await mutations.changePassword.mutateAsync({ oldPassword, newPassword })
      toast.success(revoked > 0 ? `密码已更新，其他 ${revoked} 个会话已下线` : "密码已更新")
      setOldPassword("")
      setNewPassword("")
      setConfirmPassword("")
    } catch (e) {
      setError(e instanceof Error ? e.message : "修改失败")
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base">
          <KeyRound className="size-4" /> 修改密码
        </CardTitle>
        <CardDescription>
          改完密码后，这个账号在其它设备上的登录会立即失效，当前会话保持可用。
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-5">
        <div className="grid gap-4 sm:grid-cols-3">
          <div className="grid gap-1.5">
            <Label htmlFor="profile-old-password">当前密码 *</Label>
            <Input
              id="profile-old-password"
              type="password"
              value={oldPassword}
              autoComplete="current-password"
              onChange={(event) => setOldPassword(event.target.value)}
            />
          </div>
          <div className="grid gap-1.5">
            <Label htmlFor="profile-new-password">新密码 *</Label>
            <Input
              id="profile-new-password"
              type="password"
              value={newPassword}
              autoComplete="new-password"
              onChange={(event) => setNewPassword(event.target.value)}
              placeholder={`至少 ${MIN_PASSWORD_LENGTH} 位`}
            />
          </div>
          <div className="grid gap-1.5">
            <Label htmlFor="profile-confirm-password">确认新密码 *</Label>
            <Input
              id="profile-confirm-password"
              type="password"
              value={confirmPassword}
              autoComplete="new-password"
              onChange={(event) => setConfirmPassword(event.target.value)}
            />
          </div>
        </div>

        {error && <p className="text-sm text-destructive">{error}</p>}

        <Button onClick={submit} disabled={mutations.changePassword.isPending}>
          {mutations.changePassword.isPending ? "提交中..." : "更新密码"}
        </Button>
      </CardContent>
    </Card>
  )
}

function ReadOnlyField({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <div className="grid gap-1.5">
      <span className="text-xs text-muted-foreground">{label}</span>
      <span className="text-sm">{value}</span>
      {hint && <span className="text-[11px] text-muted-foreground">{hint}</span>}
    </div>
  )
}
