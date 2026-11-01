import {
  Boxes,
  Building2,
  LayoutDashboard,
  ListTree,
  ScrollText,
  ShieldCheck,
  UserRound,
  UsersRound,
  type LucideIcon,
} from "lucide-react"

/**
 * 菜单图标：后端只下发一个 key，组件必须在这里存在于编译期。
 *
 * 这是菜单交给数据库之后唯一必须留在前端的登记。菜单管理页的「图标」下拉也从这里取，
 * 所以运营人员不会填出一个渲染不出来的 key——新增图标只需在这里加一行，
 * 下拉里立刻可选，不用改页面代码。
 *
 * 缺映射时的表现是退化成 {@link FALLBACK_ICON}，不是白屏：菜单是数据库里的数据，
 * 出现「库里有个没登记的 key」很正常（比如刚加的图标还没发版），不该让整个侧栏挂掉。
 */
export const NAV_ICONS: Record<string, LucideIcon> = {
  dashboard: LayoutDashboard,
  users: UsersRound,
  shield: ShieldCheck,
  building: Building2,
  scroll: ScrollText,
  menu: ListTree,
  user: UserRound,
}

/** 兜底图标，不进可选项——它表示「没配好」，不是一个设计选择。 */
export const FALLBACK_ICON: LucideIcon = Boxes

/** 图标下拉的选项，顺序与上面声明一致。 */
export const NAV_ICON_KEYS = Object.keys(NAV_ICONS)
