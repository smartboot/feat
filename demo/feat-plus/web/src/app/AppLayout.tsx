import { useEffect, useState } from "react"
import { Bell, Command, LogOut, Moon, Search, Sun, UserRound, UsersRound, X } from "lucide-react"
import { Link, NavLink, Navigate, Outlet, useLocation, useNavigate } from "react-router-dom"
import { toast } from "sonner"

import type { CurrentUser, NotificationItem } from "@/api/types"
import { SessionProvider } from "@/auth/session"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { CommandDialog, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList, CommandSeparator } from "@/components/ui/command"
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { Separator } from "@/components/ui/separator"
import { useAllUsers, useNotificationMutations, useNotifications, useResetDemo } from "@/services/queries"
import { FALLBACK_ICON, NAV_ICONS } from "@/lib/icons"
import { hasPermission } from "@/lib/permissions"
import { findMenuName, findRestrictedPath, navGroups } from "@/lib/menus"
import { cn } from "@/lib/utils"

/**
 * Linear-style 顶部导航。
 *
 * 传统 admin 用侧栏是因为菜单多（20+ 项），但这个系统只有 7 个页面，
 * 侧栏占 256px 宽度是浪费。改成顶部 tab 后：
 * - 内容区更宽（图表和表格不再被挤压）
 * - 视觉上更像"产品"而不是"后台"
 * - ⌘K 命令面板承担深度导航（搜索用户、快速跳转）
 */
interface NavItem {
  to: string
  label: string
  icon: React.ComponentType<{ className?: string }>
}

function toNavItem(item: { path: string; name: string; icon: string }): NavItem {
  return { to: item.path, label: item.name, icon: NAV_ICONS[item.icon] ?? FALLBACK_ICON }
}

export function AppLayout({ currentUser, onLogout, onRefreshUser }: { currentUser: CurrentUser; onLogout: () => void; onRefreshUser: () => void }) {
  const [dark, setDark] = useState(() => window.localStorage.getItem("feat-plus-theme") === "light" ? false : true)
  const [commandOpen, setCommandOpen] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()
  const canReadUsers = hasPermission(currentUser, "user:read")
  const canResetDemo = hasPermission(currentUser, "system:write")
  const { data: users = [] } = useAllUsers(canReadUsers)
  const { data: notifications = [] } = useNotifications()
  const notificationMutations = useNotificationMutations()
  const resetDemo = useResetDemo()

  useEffect(() => {
    document.documentElement.classList.toggle("light", !dark)
    document.documentElement.classList.toggle("dark", dark)
    window.localStorage.setItem("feat-plus-theme", dark ? "dark" : "light")
  }, [dark])

  useEffect(() => {
    const listener = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
        event.preventDefault()
        setCommandOpen(true)
      }
    }
    window.addEventListener("keydown", listener)
    return () => window.removeEventListener("keydown", listener)
  }, [])

  const unread = notifications.filter((item) => !item.read).length
  const menuItems = currentUser.menus ?? []
  const navSections = navGroups(menuItems).map((group) => ({
    key: group.key,
    label: group.label,
    items: group.items.map(toNavItem),
  }))
  const flatNav = navSections.flatMap((section) => section.items)
  const activeLabel = findMenuName(menuItems, location.pathname) ?? "Feat Plus"
  const blockedPath = findRestrictedPath(currentUser.restrictedPaths ?? [], location.pathname)
  const navigateAndClose = (path: string) => { navigate(path); setCommandOpen(false) }
  const handleReset = async () => { await resetDemo.mutateAsync(); toast.success("演示数据已恢复"); navigate("/dashboard") }

  useEffect(() => {
    document.title = `${activeLabel} · Feat Plus`
  }, [activeLabel])

  return (
    <div className="min-h-screen bg-background">
      {/* 顶部导航 */}
      <header className="sticky top-0 z-30 border-b bg-background/80 backdrop-blur-xl">
        <div className="mx-auto flex h-14 max-w-[1600px] items-center gap-6 px-6">
          {/* Logo */}
          <Link to="/dashboard" className="flex items-center gap-2.5 shrink-0">
            <img src="/feat-logo.svg" alt="Feat Plus" className="size-7 rounded-lg" />
            <span className="text-sm font-semibold tracking-tight">Feat Plus</span>
          </Link>

          {/* Tab 导航 */}
          <nav className="flex items-center gap-1">
            {navSections.map((section) => (
              <div key={section.key} className="flex items-center gap-1">
                {section.items.map((item) => (
                  <NavTab key={item.to} item={item} />
                ))}
              </div>
            ))}
          </nav>

          {/* 右侧工具栏 */}
          <div className="ml-auto flex items-center gap-2">
            <Button variant="ghost" size="sm" className="h-8 gap-1.5 px-2.5 text-muted-foreground" onClick={() => setCommandOpen(true)}>
              <Search className="size-3.5" />
              <span className="text-xs">搜索...</span>
              <kbd className="ml-1.5 rounded border bg-muted px-1 py-0.5 text-[10px]">⌘K</kbd>
            </Button>
            <Separator orientation="vertical" className="h-5" />
            <Button variant="ghost" size="icon" className="size-8" onClick={() => setDark((v) => !v)} aria-label="切换主题">
              {dark ? <Sun className="size-3.5" /> : <Moon className="size-3.5" />}
            </Button>
            <NotificationButton notifications={notifications} unread={unread} onRead={(id) => notificationMutations.markRead.mutate(id)} onReadAll={() => notificationMutations.markAll.mutate()} />
            <Separator orientation="vertical" className="h-5" />
            <UserMenu currentUser={currentUser} onLogout={onLogout} onOpenProfile={() => navigate("/profile")} onOpenCommand={() => setCommandOpen(true)} canResetDemo={canResetDemo} onReset={handleReset} />
          </div>
        </div>
      </header>

      {/* 内容区 */}
      <main className="mx-auto max-w-[1600px] p-6 md:p-8 lg:p-10">
        {blockedPath
          ? <NoAccessRedirect path={blockedPath} />
          : <SessionProvider user={currentUser} onRefresh={onRefreshUser}><Outlet /></SessionProvider>}
      </main>

      {/* 命令面板 */}
      <CommandDialog open={commandOpen} onOpenChange={setCommandOpen}>
        <CommandInput placeholder="输入命令或搜索用户..." />
        <CommandList>
          <CommandEmpty>没有找到匹配结果</CommandEmpty>
          <CommandGroup heading="导航">
            {flatNav.map((item, index) => {
              const Icon = item.icon
              return (
                <CommandItem key={item.to} onSelect={() => navigateAndClose(item.to)}>
                  <Icon className="size-4" />
                  {item.label}
                  <span className="ml-auto text-[10px] text-muted-foreground">⌘{index + 1}</span>
                </CommandItem>
              )
            })}
          </CommandGroup>
          <CommandSeparator />
          {canReadUsers && (
            <>
              <CommandGroup heading="用户">
                <CommandItem onSelect={() => navigateAndClose("/users")}>
                  <UsersRound className="size-4" />
                  查看全部用户
                </CommandItem>
                {users.slice(0, 5).map((user) => (
                  <CommandItem key={user.id} value={`${user.displayName} ${user.username} ${user.email}`} onSelect={() => navigateAndClose(`/users/${user.id}`)}>
                    <Avatar className="size-6">
                      <AvatarFallback>{user.displayName.slice(0, 1)}</AvatarFallback>
                    </Avatar>
                    {user.displayName}
                    <span className="text-xs text-muted-foreground">{user.username}</span>
                  </CommandItem>
                ))}
              </CommandGroup>
              <CommandSeparator />
            </>
          )}
          <CommandGroup heading="操作">
            <CommandItem onSelect={() => { navigateAndClose("/users"); toast.info("请使用用户管理页面的新建用户按钮") }}>
              <Command className="size-4" />
              新建用户
            </CommandItem>
          </CommandGroup>
        </CommandList>
      </CommandDialog>
    </div>
  )
}

function NavTab({ item }: { item: NavItem }) {
  const Icon = item.icon
  return (
    <NavLink
      to={item.to}
      className={({ isActive }) =>
        cn(
          "flex items-center gap-1.5 rounded-md px-2.5 py-1.5 text-xs font-medium transition-colors",
          isActive
            ? "bg-primary/10 text-primary"
            : "text-muted-foreground hover:bg-muted/50 hover:text-foreground"
        )
      }
    >
      <Icon className="size-3.5" />
      {item.label}
    </NavLink>
  )
}

function NoAccessRedirect({ path }: { path: string }) {
  useEffect(() => {
    toast.error("没有访问该页面的权限", { description: `页面地址：${path}` })
  }, [path])
  return <Navigate to="/dashboard" replace />
}

function NotificationButton({ notifications, unread, onRead, onReadAll }: { notifications: NotificationItem[]; unread: number; onRead: (id: number) => void; onReadAll: () => void }) {
  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button variant="ghost" size="icon" className="size-8 relative" aria-label={`通知${unread ? `，${unread} 条未读` : ""}`}>
          <Bell className="size-3.5" />
          {unread > 0 && <span className="absolute right-1.5 top-1.5 size-1.5 rounded-full bg-destructive" />}
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-80 p-0">
        <div className="flex items-center justify-between border-b p-3">
          <div>
            <p className="text-xs font-semibold">通知</p>
            <p className="text-[10px] text-muted-foreground">{unread ? `${unread} 条未读消息` : "全部已读"}</p>
          </div>
          {unread > 0 && <Button variant="ghost" size="sm" className="h-6 px-2 text-[10px]" onClick={onReadAll}>全部已读</Button>}
        </div>
        <div className="max-h-80 overflow-auto">
          {notifications.map((item) => (
            <button key={item.id} className={cn("block w-full border-b p-3 text-left last:border-0 hover:bg-muted/50", !item.read && "bg-primary/5")} onClick={() => onRead(item.id)}>
              <div className="flex items-start gap-2">
                <span className={cn("mt-1 size-1 shrink-0 rounded-full", item.read ? "bg-muted" : "bg-primary")} />
                <div>
                  <p className="text-xs font-medium">{item.title}</p>
                  <p className="mt-0.5 text-[10px] text-muted-foreground">{item.description}</p>
                  <p className="mt-1 text-[11px] text-muted-foreground">{item.createdTime}</p>
                </div>
              </div>
            </button>
          ))}
          {!notifications.length && <p className="p-6 text-center text-xs text-muted-foreground">暂无通知</p>}
        </div>
      </PopoverContent>
    </Popover>
  )
}

function UserMenu({ currentUser, onLogout, onOpenProfile, onOpenCommand, canResetDemo, onReset }: { currentUser: CurrentUser; onLogout: () => void; onOpenProfile: () => void; onOpenCommand: () => void; canResetDemo: boolean; onReset: () => void }) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" className="size-8 rounded-full p-0">
          <Avatar className="size-7">
            <AvatarFallback className="text-[10px]">{currentUser.displayName.slice(0, 1)}</AvatarFallback>
          </Avatar>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-56">
        <DropdownMenuLabel>
          <span className="block text-xs">{currentUser.displayName}</span>
          <span className="mt-0.5 block text-[10px] font-normal text-muted-foreground">{currentUser.username}</span>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem onClick={onOpenProfile} className="text-xs">
          <UserRound className="size-3.5" /> 个人中心
        </DropdownMenuItem>
        <DropdownMenuItem onClick={onOpenCommand} className="text-xs">
          <Command className="size-3.5" /> 命令面板
        </DropdownMenuItem>
        {canResetDemo && <DropdownMenuItem onClick={onReset} className="text-xs">恢复演示数据</DropdownMenuItem>}
        <DropdownMenuSeparator />
        <DropdownMenuItem onClick={onLogout} className="text-xs text-destructive">
          <LogOut className="size-3.5" /> 退出登录
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
