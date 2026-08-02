import type { AdminService } from "@/services/admin-service"
import { demoUser } from "@/mocks/seed"
import {
  createUser,
  deleteUser,
  getAllUsers,
  getDashboardSummary,
  getUser,
  listNotifications,
  listUsers,
  markAllNotificationsRead,
  markNotificationRead,
  resetDatabase,
  resetUserPassword,
  setUserStatus,
  updateUser,
} from "@/mocks/repository"

const wait = (milliseconds = 320) => new Promise((resolve) => window.setTimeout(resolve, milliseconds))

export const adminService: AdminService = {
  async login(username, password) {
    await wait(620)
    if (username !== "admin" || password !== "admin123") throw new Error("用户名或密码错误，请使用演示账号登录")
    return { accessToken: "feat-plus-demo-token", tokenType: "Bearer", expiresIn: 7200, user: demoUser }
  },
  async currentUser() {
    await wait(220)
    return demoUser
  },
  async listUsers(query) {
    await wait()
    return listUsers(query)
  },
  async getAllUsers() {
    await wait(180)
    return getAllUsers()
  },
  async getUser(id) {
    await wait(180)
    return getUser(id)
  },
  async createUser(input) {
    await wait(420)
    return createUser(input)
  },
  async updateUser(id, input) {
    await wait(420)
    return updateUser(id, input)
  },
  async setUserStatus(id, status) {
    await wait(300)
    return setUserStatus(id, status)
  },
  async resetUserPassword(id) {
    await wait(460)
    return resetUserPassword(id)
  },
  async deleteUser(id) {
    await wait(380)
    deleteUser(id)
  },
  async getDashboardSummary() {
    await wait(240)
    return getDashboardSummary()
  },
  async listNotifications() {
    await wait(160)
    return listNotifications()
  },
  async markNotificationRead(id) {
    await wait(100)
    markNotificationRead(id)
  },
  async markAllNotificationsRead() {
    await wait(120)
    markAllNotificationsRead()
  },
  async resetDemo() {
    await wait(300)
    resetDatabase()
  },
}
