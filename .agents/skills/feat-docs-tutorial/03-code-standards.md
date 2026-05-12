# 代码规范

## 代码示例规范

### 来源要求

- 必须来自真实项目：`demo/` 或 `feat-test/`
- 禁止手写未经测试的代码
- 代码必须可编译、可运行

### 代码获取流程

1. 在 `demo/` 或 `feat-test/` 中找到相关示例
2. 复制代码到文档
3. 运行验证（如可能）
4. 添加必要的注释

### JDK 8 兼容性

```java
// ❌ 禁止
var list = new ArrayList<String>();           // JDK 10+
String json = """{"name":"test"}""";          // JDK 13+
record User(String name) {}                    // JDK 14+

// ✅ 正确
List<String> list = new ArrayList<String>();
String json = "{\"name\":\"test\"}";
public class User { private String name; }
```

### 注释规范

```java
// ❌ 只说是什么
server.listen(8080);  // 监听8080端口

// ✅ 解释为什么
server.listen(8080);  // 开发环境用 8080，生产环境建议通过配置指定
```

### 异常处理

```java
// ❌ 禁止
catch (Exception e) {
    e.printStackTrace();
}

// ✅ 正确
catch (Exception e) {
    System.err.println("处理失败: " + e.getMessage());
}
```

### 代码块标注

```mdx
// ❌ 无标题代码块
\```java
code...
\```

// ✅ 有标题和路径标注
\```java title="HelloWorld.java" {5-7}
// src/main/java/com/example/HelloWorld.java
public class HelloWorld {
    public static void main(String[] args) {
        FeatCloud.cloudServer()
                .get("/", ctx -> ctx.write("Hello Feat!"))
                .listen();
    }
}
\```
```

## 步骤编写规范

### 使用 Steps 组件

```mdx
<Steps>
  <Step>
    **步骤标题**

    步骤说明文字。

    \```java
    // 代码示例
    \```
  </Step>

  <Step>
    **下一步骤标题**

    下一步骤说明。
  </Step>
</Steps>
```

### 步骤编号

- 使用中文数字：第一步、第二步
- 或使用阿拉伯数字：1. 2. 3.
- 保持全文一致

### 每个步骤包含

1. 步骤标题（做什么）
2. 步骤说明（为什么）
3. 操作指令（怎么做）
4. 验证方法（如何确认）

## 格式规范

### Aside 组件使用

**用于真正需要突出的信息：**

```mdx
<Aside type="tip">
提示信息，帮助用户更好地理解。
</Aside>

<Aside type="caution">
警告信息，提醒用户注意潜在风险。
</Aside>

<Aside type="note">
补充信息，提供额外的背景知识。
</Aside>
```

**不要滥用：**

```
❌ <Aside type="note">配置文件位于 conf/app.yml。</Aside>

✅ 配置文件位于 `conf/app.yml`。
```

### 代码块

指定语言，提高可读性：

````mdx
```java
// Java 代码
```

```bash
mvn compile
```

```yaml
server:
  port: 8080
```
````

### 表格

用于参数说明、对比等：

```mdx
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| host | String | 是 | 服务器地址 |
| port | int | 否 | 端口号，默认 8080 |
```

### 链接

```mdx
[内部文档](/feat/cloud/controller/)
[外部链接](https://maven.apache.org/)
[源码链接](https://gitee.com/smartboot/feat/blob/master/xxx)
```
