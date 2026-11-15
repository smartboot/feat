import { cleanup, fireEvent, render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, describe, expect, it, vi } from "vitest"

import type { DwFormConfig } from "@/api/types"
import { DwFormFields } from "@/components/dw/DwFormFields"
import { isFieldWritable } from "@/components/dw/meta"

// 每个用例独立渲染：不清掉上一个用例的 DOM，screen 查询会命中多份同名控件。
afterEach(cleanup)

// 下拉字段会去拉远程选项。测试只关心「选/勾了之后交出去什么」，所以把取数固定成一组带分组的选项。
// 第六个参数是 enabled：预览传 false，真实实现据此不发请求，这里也要跟着不发数据。
vi.mock("@/services/queries", () => ({
  useRemoteOptions: (
    _api: string,
    _value: string,
    _label: string,
    _group: string,
    _groupLabel: string,
    enabled = true,
  ) =>
    enabled
      ? {
          data: [
            { value: "user:read", label: "查看成员", group: "user", groupLabel: "成员" },
            { value: "user:write", label: "管理成员", group: "user", groupLabel: "成员" },
            { value: "role:read", label: "查看角色", group: "role", groupLabel: "角色" },
          ],
          isLoading: false,
          isError: false,
        }
      : { data: undefined, isLoading: false, isError: false },
}))

const form: DwFormConfig = {
  createTitle: "新建",
  editTitle: "编辑",
  createDescription: "",
  editDescription: "",
  fields: [
    { key: "code", label: "编码", widget: "input", writeMode: "createOnly" },
    { key: "name", label: "名称", widget: "input", writeMode: "always" },
    { key: "owner", label: "创建人", widget: "input", writeMode: "readonly" },
  ],
}

function control(state: "create" | "edit", key: string) {
  const { container } = render(
    <DwFormFields form={form} values={{ code: "ADMIN", name: "管理员", owner: "system" }} editing={state === "edit"} />,
  )
  return container.querySelector<HTMLInputElement>(`#dw-field-control-${key}-${form.fields.findIndex((f) => f.key === key)}`)
}

describe("写入策略的渲染", () => {
  it("仅新增可编辑：新建可填、编辑锁定", () => {
    expect(control("create", "code")?.disabled).toBe(false)
    expect(control("edit", "code")?.disabled).toBe(true)
  })

  it("始终只读：两态都锁定", () => {
    expect(control("create", "owner")?.disabled).toBe(true)
    expect(control("edit", "owner")?.disabled).toBe(true)
  })

  it("可编辑：两态都能填", () => {
    expect(control("create", "name")?.disabled).toBe(false)
    expect(control("edit", "name")?.disabled).toBe(false)
  })

  it("锁定的字段上写明原因，而不是只变灰", () => {
    const { container, rerender } = render(<DwFormFields form={form} values={{}} editing={false} />)
    expect(container.textContent).toContain("始终只读")

    rerender(<DwFormFields form={form} values={{}} editing />)
    expect(container.textContent).toContain("创建后不可改")
  })

  it("旧配置只有 immutable 时按 createOnly 处理", () => {
    const legacy = { key: "code", label: "编码", widget: "input" as const, immutable: true }
    expect(isFieldWritable(legacy, false)).toBe(true)
    expect(isFieldWritable(legacy, true)).toBe(false)
  })
})

const multiForm: DwFormConfig = {
  createTitle: "新建",
  editTitle: "编辑",
  createDescription: "",
  editDescription: "",
  fields: [
    {
      key: "permissions",
      label: "权限点",
      widget: "select",
      multiple: true,
      optionsApi: "/api/permissions",
      optionValueField: "key",
      optionLabelField: "label",
      optionGroupField: "groupKey",
      optionGroupLabelField: "groupLabel",
    },
  ],
}

describe("多选字段", () => {
  it("渲染成勾选组而不是下拉，勾选后交出的是数组", () => {
    const onChange = vi.fn()
    render(
      <DwFormFields form={multiForm} values={{ permissions: ["user:read"] }} onChange={onChange} editing />,
    )
    expect(screen.getByRole("checkbox", { name: /查看成员/ })).toHaveAttribute("aria-checked", "true")
    expect(screen.getByRole("checkbox", { name: /管理成员/ })).toHaveAttribute("aria-checked", "false")

    fireEvent.click(screen.getByRole("checkbox", { name: /管理成员/ }))
    // 顺序跟随选项顺序，不是点击顺序：否则每次勾选都会让提交值顺序漂移。
    expect(onChange).toHaveBeenCalledWith("permissions", ["user:read", "user:write"])
  })

  it("按分组字段分块，并能整组勾选", () => {
    const onChange = vi.fn()
    render(<DwFormFields form={multiForm} values={{ permissions: [] }} onChange={onChange} editing />)
    expect(screen.getByText("成员")).toBeTruthy()
    expect(screen.getByText("角色")).toBeTruthy()

    // 点「成员」组的全选，只勾这一组，不波及「角色」组。
    fireEvent.click(screen.getAllByText("全选本组")[0])
    expect(onChange).toHaveBeenCalledWith("permissions", ["user:read", "user:write"])
  })

  it("取消勾选后从数组里移除，而不是留下空串", () => {
    const onChange = vi.fn()
    render(
      <DwFormFields
        form={multiForm}
        values={{ permissions: ["user:read", "user:write"] }}
        onChange={onChange}
        editing
      />,
    )
    fireEvent.click(screen.getByRole("checkbox", { name: /查看成员/ }))
    expect(onChange).toHaveBeenCalledWith("permissions", ["user:write"])
  })
})

const singleForm: DwFormConfig = {
  createTitle: "新建",
  editTitle: "编辑",
  createDescription: "",
  editDescription: "",
  fields: [
    {
      key: "owner",
      label: "负责人",
      widget: "select",
      optionsApi: "/api/users",
      optionValueField: "id",
      optionLabelField: "name",
    },
  ],
}

/**
 * 单选下拉也吃 optionsApi。
 *
 * 这里曾经漏过：配置器允许任何 select 选「接口现取」，但渲染层只给多选做了远程取数，
 * 单选直接读 `field.options`（切到接口模式时它被清空了）。表现为保存成功、预览正常
 * （预览也是空的，看不出差别）、上线后下拉里只有一个 placeholder——最难查的一类故障。
 */
describe("单选下拉", () => {
  it("配了 optionsApi 时用接口返回的选项，选中后交出字符串值", async () => {
    const onChange = vi.fn()
    render(<DwFormFields form={singleForm} values={{ owner: "" }} onChange={onChange} editing />)
    await userEvent.click(document.getElementById("dw-field-control-owner-0") as HTMLElement)
    await userEvent.click(await screen.findByRole("option", { name: "查看成员" }))
    expect(onChange).toHaveBeenCalledWith("owner", "user:read")
  })

  it("预览（remote=false）不取数，改成说明选项来源——而不是伪装成空下拉", () => {
    render(<DwFormFields form={singleForm} values={{ owner: "" }} editing disabled remote={false} />)
    expect(screen.getByText(/选项来自 \/api\/users，预览不取数/)).toBeTruthy()
    expect(screen.queryByRole("option", { name: "查看成员" })).toBeNull()
  })
})
