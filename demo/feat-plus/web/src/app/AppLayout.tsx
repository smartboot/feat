import { useEffect, useState } from "react"
import { Bell, Boxes, Building2, ChevronDown, Command, LayoutDashboard, LogOut, Menu, Moon, PanelLeftClose, PanelLeftOpen, ScrollText, Search, ShieldCheck, Sun, UserRound, UsersRound, X } from "lucide-react"
import type { LucideIcon } from "lucide-react"
import { Link, NavLink, Outlet, useLocation, useNavigate } from "react-router-dom"
import { toast } from "sonner"

import type { CurrentUser, NotificationItem } from "@/api/types"
import { SessionProvider } from "@/auth/session"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { CommandDialog, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList, CommandSeparator, CommandShortcut } from "@/components/ui/command"
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { Separator } from "@/components/ui/separator"
import { useAllUsers, useNotificationMutations, useNotifications, useResetDemo } from "@/services/queries"
import { hasPermission } from "@/lib/permissions"
import { findMenuName, navGroups } from "@/lib/menus"
import { cn } from "@/lib/utils"

/**
 * 菜单的几何形状只剩两样：路径、标题、图标。分组与顺序由后端下发。
 *
 * 不再有 primaryNav / systemNav / PAGE_TITLES 三份硬编码——它们曾让「加一个模块」要在前端改三处，
 * 而且漏一处不报错：菜单不出现但 URL 能打开，或者点进去被 catch-all 弹回首页。
 */
interface NavItem {
  to: string
  label: string
  icon: LucideIcon
}

/**
 * 图标是唯一必须留在前端的菜单属性：后端只能下发一个 key，组件必须在编译期存在。
 * 新增菜单时如果忘了在这里加一项，会退化成默认图标而不是白屏——由 check_menu.py 在 CI 里提示。
 */
const NavIcons: Record<string, LucideIcon> = {
  dashboard: LayoutDashboard,
  users: UsersRound,
  shield: ShieldCheck,
  building: Building2,
  scroll: ScrollText,
  user: UserRound,
}

function toNavItem(item: { path: string; name: string; icon: string }): NavItem {
  return { to: item.path, label: item.name, icon: NavIcons[item.icon] ?? Boxes }
}

export function AppLayout({ currentUser, onLogout }: { currentUser: CurrentUser; onLogout: () => void }) {
  const [mobileOpen, setMobileOpen] = useState(false)
  const [collapsed, setCollapsed] = useState(() => window.localStorage.getItem("feat-plus-sidebar") === "collapsed")
  const [dark, setDark] = useState(() => window.localStorage.getItem("feat-plus-theme") === "dark")
  const [commandOpen, setCommandOpen] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()
  const canReadUsers = hasPermission(currentUser, "user:read")
  const canResetDemo = hasPermission(currentUser, "system:write")
  // 没有 user:read 的角色（如普通成员）连命令面板里的用户搜索也不该发起请求，否则只会拿到 403。
  const { data: users = [] } = useAllUsers(canReadUsers)
  const { data: notifications = [] } = useNotifications()
  const notificationMutations = useNotificationMutations()
  const resetDemo = useResetDemo()

  useEffect(() => {
    document.documentElement.classList.toggle("dark", dark)
    window.localStorage.setItem("feat-plus-theme", dark ? "dark" : "light")
  }, [dark])
  useEffect(() => {
    const listener = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") { event.preventDefault(); setCommandOpen(true) }
    }
    window.addEventListener("keydown", listener)
    return () => window.removeEventListener("keydown", listener)
  }, [])

  const unread = notifications.filter((item) => !item.read).length
  // 菜单只有一个来源：后端按权限过滤后随登录/me 下发。这里只负责分组和图标映射。
  const menuItems = currentUser.menus ?? []
  const navSections = navGroups(menuItems).map((group) => ({
    key: group.key,
    label: group.label,
    items: group.items.map(toNavItem),
  }))
  const flatNav = navSections.flatMap((section) => section.items)
  const activeLabel = findMenuName(menuItems, location.pathname) ?? "Feat Plus"
  const navigateAndClose = (path: string) => { navigate(path); setMobileOpen(false); setCommandOpen(false) }
  const handleReset = async () => { await resetDemo.mutateAsync(); toast.success("演示数据已恢复"); navigate("/dashboard") }

  // 标题由各页面自己渲染，布局里只同步浏览器标签页标题，避免屏幕上出现两个同名大标题。
  useEffect(() => {
    document.title = `${activeLabel} · Feat Plus`
  }, [activeLabel])

  return (
    <div className="min-h-screen bg-background">
      {mobileOpen && <button className="fixed inset-0 z-40 bg-black/50 md:hidden" aria-label="关闭菜单" onClick={() => setMobileOpen(false)} />}
      <aside className={cn("fixed inset-y-0 left-0 z-50 flex w-64 flex-col border-r bg-sidebar text-sidebar-foreground transition-[width,transform] duration-200 md:translate-x-0", mobileOpen ? "translate-x-0" : "-translate-x-full", collapsed ? "md:w-16" : "md:w-64")}>
        <div className="flex h-16 items-center gap-3 border-b px-4">
          <Link to="/dashboard" className="flex min-w-0 items-center gap-3" onClick={() => setMobileOpen(false)}>
            <img src="/feat-logo.svg" alt="Feat Plus" className="size-8 rounded-lg" />
            {!collapsed && <span className="grid min-w-0"><strong className="truncate text-sm">Feat Plus</strong><span className="truncate text-[10px] uppercase tracking-[.14em] text-muted-foreground">Admin Console</span></span>}
          </Link>
          <Button className="ml-auto md:hidden" variant="ghost" size="icon" onClick={() => setMobileOpen(false)}><X className="size-4" /></Button>
        </div>
        <nav className="flex-1 space-y-6 overflow-y-auto p-3">
          {navSections.map((section) => (
            <div key={section.key} className="space-y-1">
              {!collapsed && <p className="px-2 pb-2 text-[10px] font-semibold uppercase tracking-[.14em] text-muted-foreground">{section.label}</p>}
              {section.items.map((item) => <NavEntry key={item.to} item={item} collapsed={collapsed} onNavigate={() => setMobileOpen(false)} />)}
            </div>
          ))}
        </nav>
        <div className="border-t p-3">
          <DropdownMenu>
            <DropdownMenuTrigger asChild><button className={cn("flex w-full items-center gap-3 rounded-md p-2 text-left hover:bg-sidebar-accent", collapsed && "justify-center p-1")}><Avatar className="size-8"><AvatarFallback>{currentUser.displayName.slice(0, 1)}</AvatarFallback></Avatar>{!collapsed && <><span className="grid min-w-0 flex-1"><strong className="truncate text-xs">{currentUser.displayName}</strong><span className="truncate text-[10px] text-muted-foreground">{currentUser.username}</span></span><ChevronDown className="size-4 text-muted-foreground" /></>}</button></DropdownMenuTrigger>
            <DropdownMenuContent side="right" align="end">
              <DropdownMenuLabel>
                <span className="block">{currentUser.displayName}</span>
                <span className="mt-0.5 block text-[11px] font-normal text-muted-foreground">
                  {currentUser.roleNames.join(" / ") || "无角色"}
                </span>
                <span className="block text-[11px] font-normal text-muted-foreground">
                  数据范围：{currentUser.scopeDescription}
                </span>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={() => navigate("/profile")}><UserRound /> 个人中心</DropdownMenuItem>
              <DropdownMenuItem onClick={() => setDark((value) => !value)}>{dark ? <Sun /> : <Moon />} 切换主题</DropdownMenuItem>
              {canResetDemo && <DropdownMenuItem onClick={handleReset}>恢复演示数据</DropdownMenuItem>}
              <DropdownMenuSeparator />
              <DropdownMenuItem destructive onClick={onLogout}><LogOut /> 退出登录</DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </aside>
      <div className={cn("min-h-screen transition-[padding] duration-200 md:pl-64", collapsed && "md:pl-16")}>
        <header className="sticky top-0 z-30 flex h-16 items-center gap-3 border-b bg-background/80 px-4 backdrop-blur-xl md:px-6">
          <Button className="md:hidden" variant="ghost" size="icon" onClick={() => setMobileOpen(true)}><Menu className="size-5" /></Button>
          <Button className="hidden md:inline-flex" variant="ghost" size="icon" onClick={() => { setCollapsed((value) => !value); window.localStorage.setItem("feat-plus-sidebar", collapsed ? "expanded" : "collapsed") }} aria-label={collapsed ? "展开侧栏" : "折叠侧栏"}>{collapsed ? <PanelLeftOpen className="size-4" /> : <PanelLeftClose className="size-4" />}</Button>
          <Button variant="outline" className="hidden h-9 w-80 justify-start gap-2 text-muted-foreground shadow-none sm:inline-flex" onClick={() => setCommandOpen(true)}><Search className="size-4" /><span>搜索菜单、用户或操作...</span><kbd className="ml-auto rounded border bg-muted px-1.5 py-0.5 text-[10px]">⌘ K</kbd></Button>
          <div className="ml-auto flex items-center gap-1">
            <Button variant="ghost" size="icon" onClick={() => setDark((value) => !value)} aria-label="切换主题">{dark ? <Sun className="size-4" /> : <Moon className="size-4" />}</Button>
            <NotificationButton notifications={notifications} unread={unread} onRead={(id) => notificationMutations.markRead.mutate(id)} onReadAll={() => notificationMutations.markAll.mutate()} />
            <Separator orientation="vertical" className="mx-2 hidden h-6 sm:block" />
            <DropdownMenu><DropdownMenuTrigger asChild><Button variant="ghost" className="gap-2 px-2"><Avatar className="size-7"><AvatarFallback>{currentUser.displayName.slice(0, 1)}</AvatarFallback></Avatar><span className="hidden text-left sm:grid"><strong className="text-xs">{currentUser.displayName}</strong><span className="truncate text-[10px] text-muted-foreground">{currentUser.roleNames.join(" / ") || "无角色"}</span></span><ChevronDown className="hidden size-3.5 text-muted-foreground sm:block" /></Button></DropdownMenuTrigger><DropdownMenuContent align="end"><DropdownMenuLabel><span className="block">{currentUser.username}</span><span className="mt-1 block text-[11px] font-normal text-muted-foreground">数据范围：{currentUser.scopeDescription}</span></DropdownMenuLabel><DropdownMenuSeparator /><DropdownMenuItem onClick={() => navigate(`/users/${currentUser.id}`)}>查看我的账号</DropdownMenuItem><DropdownMenuItem onClick={() => setCommandOpen(true)}>打开命令面板</DropdownMenuItem><DropdownMenuSeparator /><DropdownMenuItem destructive onClick={onLogout}><LogOut /> 退出登录</DropdownMenuItem></DropdownMenuContent></DropdownMenu>
          </div>
        </header>
        <main className="mx-auto w-full max-w-[1600px] p-4 md:p-6 lg:p-8"><SessionProvider user={currentUser}><Outlet /></SessionProvider></main>
      </div>
      <CommandDialog open={commandOpen} onOpenChange={setCommandOpen}><CommandInput placeholder="输入命令或搜索用户..." /><CommandList><CommandEmpty>没有找到匹配结果</CommandEmpty><CommandGroup heading="导航">{flatNav.map((item, index) => { const Icon = item.icon; return <CommandItem key={item.to} onSelect={() => navigateAndClose(item.to)}><Icon className="size-4" />{item.label}<CommandShortcut>⌘ {index + 1}</CommandShortcut></CommandItem> })}</CommandGroup><CommandSeparator />{canReadUsers && <><CommandGroup heading="用户"><CommandItem onSelect={() => navigateAndClose("/users")}><UsersRound className="size-4" />查看全部用户</CommandItem>{users.slice(0, 5).map((user) => <CommandItem key={user.id} value={`${user.displayName} ${user.username} ${user.email}`} onSelect={() => navigateAndClose(`/users/${user.id}`)}><Avatar className="size-6"><AvatarFallback>{user.displayName.slice(0, 1)}</AvatarFallback></Avatar>{user.displayName}<span className="text-xs text-muted-foreground">{user.username}</span></CommandItem>)}</CommandGroup><CommandSeparator /></>}<CommandGroup heading="操作"><CommandItem onSelect={() => { navigateAndClose("/users"); toast.info("请使用用户管理页面的新建用户按钮") }}><Command className="size-4" />新建用户</CommandItem></CommandGroup></CommandList></CommandDialog>
    </div>
  )
}

function NotificationButton({ notifications, unread, onRead, onReadAll }: { notifications: NotificationItem[]; unread: number; onRead: (id: number) => void; onReadAll: () => void }) {
  return <Popover><PopoverTrigger asChild><Button variant="ghost" size="icon" className="relative" aria-label={`通知${unread ? `，${unread} 条未读` : ""}`}><Bell className="size-4" />{unread > 0 && <span className="absolute right-1.5 top-1.5 size-2 rounded-full bg-destructive ring-2 ring-background" />}</Button></PopoverTrigger><PopoverContent align="end" className="w-80 p-0"><div className="flex items-center justify-between border-b p-4"><div><p className="text-sm font-semibold">通知</p><p className="text-xs text-muted-foreground">{unread ? `${unread} 条未读消息` : "全部已读"}</p></div>{unread > 0 && <Button variant="ghost" size="sm" onClick={onReadAll}>全部已读</Button>}</div><div className="max-h-80 overflow-auto">{notifications.map((item) => <button key={item.id} className={cn("block w-full border-b p-4 text-left last:border-0 hover:bg-muted/50", !item.read && "bg-primary/5")} onClick={() => onRead(item.id)}><div className="flex items-start gap-2"><span className={cn("mt-1.5 size-1.5 shrink-0 rounded-full", item.read ? "bg-muted" : "bg-primary")} /><div><p className="text-sm font-medium">{item.title}</p><p className="mt-1 text-xs text-muted-foreground">{item.description}</p><p className="mt-2 text-[10px] text-muted-foreground">{item.createdTime}</p></div></div></button>)}{!notifications.length && <p className="p-8 text-center text-sm text-muted-foreground">暂无通知</p>}</div></PopoverContent></Popover>
}

/**
 * 侧边栏导航项。能否看到由调用方按权限过滤，这里只负责渲染与高亮。
 */
function NavEntry({ item, collapsed, onNavigate }: { item: NavItem; collapsed: boolean; onNavigate: () => void }) {
  const Icon = item.icon
  return (
    <NavLink
      to={item.to}
      title={collapsed ? item.label : undefined}
      onClick={onNavigate}
      className={({ isActive }) =>
        cn(
          "flex h-9 items-center gap-3 rounded-md px-3 text-sm transition-colors",
          isActive
            ? "bg-sidebar-accent font-medium text-sidebar-accent-foreground"
            : "text-muted-foreground hover:bg-sidebar-accent/70 hover:text-sidebar-accent-foreground",
          collapsed && "justify-center px-0",
        )
      }
    >
      <Icon className="size-4 shrink-0" />
      <span className={cn(collapsed && "sr-only")}>{item.label}</span>
    </NavLink>
  )
}
