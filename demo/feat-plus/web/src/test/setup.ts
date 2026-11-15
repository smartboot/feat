import "@testing-library/jest-dom/vitest"

/**
 * Radix 的下拉 / 菜单在 jsdom 里依赖几项没实现的 DOM API：
 * 打开时要 `hasPointerCapture`，定位选中项要 `scrollIntoView`，测尺寸要 `ResizeObserver`。
 * 不补齐这些，交互测试就只能绕开「点开下拉」这一步——而配置页的核心动作恰好都在下拉里。
 */
if (!Element.prototype.hasPointerCapture) {
  Element.prototype.hasPointerCapture = () => false
}
if (!Element.prototype.setPointerCapture) {
  Element.prototype.setPointerCapture = () => {}
}
if (!Element.prototype.releasePointerCapture) {
  Element.prototype.releasePointerCapture = () => {}
}
if (!Element.prototype.scrollIntoView) {
  Element.prototype.scrollIntoView = () => {}
}
if (!globalThis.ResizeObserver) {
  globalThis.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  } as unknown as typeof ResizeObserver
}
