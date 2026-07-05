# 在 Windows 上用 BlackBerry JDE 构建 bbtime.cod

这份文档教你在 Windows 上装好 BlackBerry JDE,把本仓库的源码编译成可安装的
`bbtime.cod`,并配置"每天后台自启动同步"。

---

## 0. 速查（TL;DR）

| 项目 | 选择 |
|------|------|
| 目标系统 | BlackBerry OS 4.5 及以上 |
| JDE 版本 | **JDE 4.5.0**（从你给的 archive 下 `.exe` 安装包） |
| **JDK 版本** | **JDK 5.0（1.5），32 位** — 必须先装 |
| 安装顺序 | **先 JDK，后 JDE** |
| 代码签名 | **不需要**（本项目没用任何受控 API） |
| 产物 | `bbtime.cod`（配 `bbtime.alx` 安装到真机） |

> ⚠️ 关键：**不要用 JDK 1.7 及以上**。JDE 4.5 的 `rapc` 编译器会解析 class
> 文件,新版 JDK 生成的字节码会让它报 `NullPointerException`、生成空的/损坏的
> `.cod`,或直接编译失败。JDE 4.5 就用 **JDK 1.5**。

---

## 1. 关于 Java 版本：必须 1.5 吗？

**对 JDE 4.5,是的,请用 JDK 1.5（32 位)。** 这是官方对应、兼容性最好的组合。

各 JDE 版本对应的 JDK：

| JDE 版本 | 推荐 JDK |
|----------|----------|
| 4.1 – 4.3 | JDK 1.4.2（32 位） |
| **4.5 – 4.7** | **JDK 5.0 / 1.5（32 位）** |
| 5.0 | JDK 6 / 1.6（32 位） |
| 6.0 – 7.1 | JDK 6 / 1.6（32 位） |

要点说明：

- **为什么不能用新版**：`rapc` 既调用 JDK 的 `javac`,又自己解析编译出来的
  class 文件做 RIM 私有处理。JDK 1.7+ 改了 class 文件格式/校验方式,`rapc`
  处理不了 → 报错或产出坏 cod。这是最常见的"能装但编不出来"的坑。
- **为什么 32 位**：JDE 4.5 的工具链(`rapc.exe`、`preverify.exe`、
  `javaloader.exe`、模拟器)都是 32 位程序,配 32 位 JVM 最省事。64 位 JDK 常
  引发奇怪问题。
- 用 1.6 有时也能编 4.5 的项目,但**不保证**;要稳就 1.5。

---

## 2. 下载

1. **BlackBerry JDE 4.5.0**：从你贴的 archive
   （`archive.lunar-project.org/BlackBerry OS/Development/JDE/`）下载 4.5.0 的
   安装包（形如 `BlackBerry_JDE_4.5.0_*.exe`）。
2. **JDK 5.0（32 位,Windows i586）**：从 Oracle 的 Java Archive 获取（需登录
   Oracle 账号）。文件名形如 `jdk-1_5_0_22-windows-i586.exe`。
   （下面路径示例都按 `jdk1.5.0_22` 写,你的小版本号可能不同,按实际改。）

---

## 3. 安装（顺序很重要）

### 3.1 先装 JDK 1.5

1. 运行 `jdk-1_5_0_22-windows-i586.exe`,默认装到
   `C:\Program Files (x86)\Java\jdk1.5.0_22\`。
2. 设环境变量（控制面板 → 系统 → 高级 → 环境变量）：
   - 新建 `JAVA_HOME` = `C:\Program Files (x86)\Java\jdk1.5.0_22`
   - 在 `Path` 里加一条 `%JAVA_HOME%\bin`
3. 开一个**新的** cmd 窗口验证：
   ```bat
   java -version
   ```
   应输出 `java version "1.5.0_22"` 之类。

### 3.2 再装 JDE 4.5.0

1. **以管理员身份**运行 JDE 4.5.0 安装包。
2. 安装时若提示选择 JDK,指向上面的 `jdk1.5.0_22`。
3. 默认装到
   `C:\Program Files (x86)\Research In Motion\BlackBerry JDE 4.5.0\`。

装完后,这个目录里你会看到：

```
BlackBerry JDE 4.5.0\
├─ bin\      rapc.exe, preverify.exe, javaloader.exe, JdwpConsole..., JDE.exe(IDE)
├─ lib\      net_rim_api.jar      ← 编译要 import 的 API 库
├─ simulator\  各机型模拟器(*.cod 等)
└─ samples\
```

> Windows 10/11 上这套 2008 年的软件通常能跑;若 IDE 或模拟器启动异常,右键
> 以管理员运行,或在"兼容性"里设成 Windows 7 兼容模式。

---

## 4. 方式一：用 JDE 图形界面构建（推荐，能配自启动）

### 4.1 建工作区和项目

1. 启动 IDE：运行 `bin\JDE.exe`（或开始菜单的 BlackBerry JDE 4.5.0）。
2. `File → New → Workspace...`,建一个工作区（比如 `bbtime.jdw`）。
3. 在工作区上右键 `Create Project...`,建主项目 `bbtime`。
4. 打开项目属性（右键项目 → `Properties` → `Build` 选项卡）：
   - **Project type** 选 **Application**（CLDC 应用,有图标可点开）。
5. 把源码加进来：右键项目 → `Add File to Project...`,选中本仓库 `src\` 下
   **全部** `.java` 文件（包结构由文件里的 `package` 语句决定,JDE 会按包组织,
   不用手动建目录）。要加的文件：
   ```
   src\com\bbtime\TimeSyncApp.java
   src\com\bbtime\ui\TimeSyncScreen.java
   src\com\bbtime\net\*.java
   src\com\bbtime\core\*.java
   src\com\bbtime\bg\DailyCheckDaemon.java
   ```
   > 注意：**不要**加 `ci\stubs\` 里的任何文件——那些只是给云端 CI 做类型检查
   > 的假接口,真机编译要用 `lib\net_rim_api.jar`,两者会冲突。

6. （可选）设应用标题/图标：项目属性 `Application` 选项卡里可填 Title;
   `Resources` 里可挂一个 80×80 左右的图标 png（没有也能编,会用默认图标）。
7. 菜单 `Build → Build`。成功后在项目目录下得到：
   ```
   bbtime.cod   ← 安装用这个
   bbtime.cso  bbtime.jar  bbtime.jad  bbtime.debug
   ```

### 4.2 配置"每天后台自启动"（alternate entry point）

本 App 的 `main()` 会看第一个启动参数：无参 → 打开界面;参数是 `autostart` →
只在后台跑守护线程（`DailyCheckDaemon`,开机 60 秒后 + 之后每 24 小时检查一次）。
要让它开机自动进后台,得再建一个**备用入口点**项目：

1. 在同一个工作区右键 `Create Project...`,建第二个项目 `bbtime-autostart`。
2. 打开它的属性 → `Build` 选项卡：
   - **Project type** 选 **Alternate CLDC Application Entry Point**。
   - **Alternate entry point for** 选 `bbtime`（上面的主项目）。
   - **Application argument(s)** 填 `autostart`。
3. 到 `Application` 选项卡：
   - 勾 **Auto-run on startup**（开机自启）。
   - 勾 **Do not display the application icon on the Home screen**（后台入口不显示
     图标,界面还是走主项目那个图标）。
   - Startup tier 保持默认（7）。
4. 再 `Build → Build` 整个工作区。这样生成的 `bbtime.cod` 就同时带两个入口：
   点图标开界面 / 开机自动进后台。

> 两个项目共用一个模块,所以最终还是**一个** `bbtime.cod`。

### 4.3 在模拟器里跑

1. 菜单 `Build → Build and Run`（或按运行键）启动模拟器,App 会装进去。
2. **联网测试要点**：模拟器默认不通外网,需要另开
   **MDS-CS 模拟器**（开始菜单 → BlackBerry JDE 4.5.0 → MDS Simulator，
   或 JDE 里 `Simulator → Start MDS`）。HTTP/HTTPS/socket 才能出网。
   - 或者在 App 的 **Transport** 里选 `Direct TCP`（对应 `;deviceside=true`）
     直连,不走 MDS。
3. 打开 bbtime,选协议(先试 NTP / `pool.ntp.org`),按 **`S`** 手动同步,看
   状态栏显示的"正确时间/偏差"。

---

## 5. 方式二：命令行构建（Git Bash）

装了 [Git for Windows](https://git-scm.com/) 后,用自带的 Git Bash：

```sh
# 路径按你的实际安装位置改；注意含空格要整体加引号
BB_JDE="/c/Program Files (x86)/Research In Motion/BlackBerry JDE 4.5.0" ./build.sh
```

`build.sh` 会自动找到 `bin/rapc.exe` 和 `lib/net_rim_api.jar`,把 `src/` 全部
编译,产出 `build/bbtime.cod`。

> 命令行方式**难以配置备用入口点(自启动)**,所以"开机自启动"那部分请用
> 方式一的 IDE 来做。命令行适合快速出一个"能手动同步"的 cod。

手动直接调 `rapc.exe` 也行(build.sh 内部就是这么拼的),但要手动列出所有 `.java`
文件,比较麻烦,不推荐。

---

## 6. 装到真机

出好的 `bbtime.cod` 有两种装法：

**A. BlackBerry Desktop Manager（图形化，推荐新手）**
1. 把 `bbtime.cod` 和仓库里的 `bbtime.alx` 放**同一个目录**。
   `bbtime.alx` 里 `Java="1.45"` 表示要求 OS 4.5+。
2. 手机用 USB 连电脑,打开 Desktop Manager → Applications / Application Loader,
   选中 `bbtime.alx`,按向导安装。

**B. 命令行 javaloader（快)**
```bat
cd "C:\Program Files (x86)\Research In Motion\BlackBerry JDE 4.5.0\bin"
javaloader.exe -u load "C:\path\to\bbtime.cod"
```
`-u` 表示走 USB。装完在手机上找到 bbtime 图标。

---

## 7. 关于代码签名（真机必须）

**真机上本项目必须签名。** 因为 `ClockSetter` 用了受控 API
`Device.setDateTime(long)`——受控 API 未签名时在真机上会抛
`ControlledAccessException`。

- **模拟器**:受控 API 不用签名,直接能跑,可以先在模拟器里验证"按 S 改时间"。
- **真机**:cod 装上去要能改时间,必须用 RIM 签名密钥签过。流程是向
  BlackBerry 申请签名密钥,再用 JDE 里的 `SignatureTool.jar`（或菜单
  `Build → Request/Sign`)对 cod 签名。

> ⚠️ 2026 年的现实:RIM 的签名服务器早已关停,**现在基本申请不到新的签名密钥**。
> 如果你没有当年留下的密钥,真机上 `Device.setDateTime` 会被受控 API 拦住——这时
> App 会回退到"显示正确时间 + 提示手动对表"。这是目前最大的实际障碍,不是代码问题。
>
> 其余用到的 API(网络 `Connector`、持久化 `PersistentStore`、UI、后台
> `Application`)本身不需要签名;需要签名的只有 `Device.setDateTime` 这一处。

---

## 8. 常见问题（FAQ）

**Q：rapc 报 `NullPointerException` / 生成的 cod 是空的 / 装到机器上闪退。**
A：几乎都是 **JDK 版本太新**。确认 `java -version` 是 **1.5**,而不是 8/11/17。
系统里装了多个 JDK 时,把 `JAVA_HOME` 和 `Path` 指向 1.5,重开 cmd 再编。

**Q：装 JDE 时提示找不到 JDK / 装完打不开。**
A：先装好 **32 位 JDK 1.5** 并设 `JAVA_HOME`,再装 JDE。JDE 是个 Java 程序,靠
这个 JDK 启动。

**Q：用了 64 位 JDK,工具行为异常。**
A：换 **32 位** JDK 1.5。

**Q：模拟器里按 S 同步一直超时 / 网络不通。**
A：要么启动 **MDS Simulator** 再试 HTTP/HTTPS;要么在 Transport 里选
`Direct TCP`(`;deviceside=true`)直连。真机上同理:看你走 Wi-Fi 还是运营商
数据,在 Transport 里选 `Wi-Fi` / `Direct TCP` / `BIS/MDS` 试。

**Q：能自动改系统时间吗？**
A：**能**。用的是 RIM API `net.rim.device.api.system.Device.setDateTime(long)`
（`static boolean`,自 BlackBerry API 3.6.0 起就有,所以 4.5+ 都能用),按 `S`
会真正写入系统时间。但真机上有两个前提(模拟器都不需要):

1. **必须代码签名**。`Device.setDateTime` 是受控 API,未签名会抛
   `ControlledAccessException`,所以真机装的 cod 必须用 RIM 签名密钥签过。
2. **先关掉自动网络对时**。到 `选项 → 日期/时间`,把"更新时间"改成 `手动`,
   否则系统会用运营商网络时间(NITZ)把你改的覆盖掉。企业 **IT Policy** 也可能
   直接禁止改时间。

设置失败时 bbtime 不会假装成功,会在状态栏说明,并弹出精确时间让你手动对。
注意 `Device.setTimeZone()` 是 4.6.0 才有,针对 4.5 我们只设绝对时间(UTC),
时区留给用户自己在系统里设。

---

## 9. 一句话总结

装 **32 位 JDK 1.5** → 装 **JDE 4.5.0** → 在 IDE 里建 `Application` 主项目
（导入 `src/`）+ 一个 `autostart` 备用入口点项目 → Build 出 `bbtime.cod` →
配 `bbtime.alx` 装机。别用 JDK 1.7+。
