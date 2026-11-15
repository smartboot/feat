import { render } from "@testing-library/react"
import { describe, expect, it } from "vitest"

import type { DwFormConfig } from "@/api/types"
import { DwFormFields } from "@/components/dw/DwFormFields"
import { isFieldWritable } from "@/components/dw/meta"

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
