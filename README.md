# 智能客服工单系统（smart-ticket）

> 一个 **Java 后端 + Python AI 服务** 的前后端分离项目，用来做简历项目 / 毕业设计。
> 技术栈：Spring Boot 3.5 + MyBatis-Plus + MySQL 8 + Redis + JWT + FastAPI

---

## 一、这个项目能帮你回答哪些面试题

这不是"又一个 CRUD 管理系统"。下面每一行，都是面试官会问的，而你在项目里**真的有对应代码**：

| 面试问题 | 项目里的答案 |
|---|---|
| 你的项目怎么做的登录鉴权？ | JWT + 拦截器，见 `JwtUtil` / `JwtInterceptor` |
| JWT 和 Session 有什么区别？ | 注释里写了：JWT 存客户端，服务端无状态，天然适合分布式 |
| 你们怎么做统一返回格式？ | `Result<T>` 泛型封装 |
| 你们怎么做全局异常处理？ | `@RestControllerAdvice` + 自定义 `BizException` |
| Redis 在你的项目里用在哪？ | 缓存首页统计，Cache Aside 模式，见 `TicketServiceImpl.stats()` |
| 缓存一致性怎么保证？ | 数据变更时主动删缓存 `clearStatsCache()` |
| Redis 挂了怎么办？ | 降级直接查数据库，try-catch 兜底，接口不会 500 |
| 分页怎么实现的？ | MyBatis-Plus 分页插件 + `PaginationInnerInterceptor` |
| 逻辑删除怎么做？ | `@TableLogic` 注解，delete 变成 update |
| Controller 和 Service 怎么分工？ | Controller 只接参数、调 Service、包返回值 |
| AI 能力怎么集成进 Java 项目的？ | Java 通过 HTTP 调 Python FastAPI 服务，见 `ai-service/main.py` |
| 调用第三方接口要注意什么？ | 必须设 timeout、必须做失败降级（本项目两处都做了） |
| 跨域怎么解决？ | `WebConfig` 里的 CORS 配置 |

---

## 二、环境要求

| 软件 | 版本 | 你机器上的状态 |
|---|---|---|
| JDK | 17+（本项目用 25） | ✅ 已装 JDK 25 |
| Maven | 3.6+ | ✅ 已装 3.9.4 |
| MySQL | 8.x | ✅ 已装 8.0.44（**服务需要手动启动**） |
| Redis | 5.x+ | ✅ 已装（**服务需要手动启动**） |
| Python | 3.9+ | ✅ 已装 3.13 |
| IDEA | 2023+ | ✅ 已装 2025.1 |

---

## 三、启动步骤（照着做，5 分钟）

### 第 1 步：启动 MySQL 和 Redis

**用管理员身份**打开 PowerShell（右键开始菜单 → 终端(管理员)），执行：

```powershell
net start MySQL80
net start Redis
```

> 如果提示服务名不对，先执行 `Get-Service *mysql*,*redis*` 看真实名字。

### 第 2 步：建数据库

在项目根目录执行（会提示输入 MySQL 密码）：

```powershell
mysql -uroot -p < sql\schema.sql
```

或者用 Navicat / IDEA 的 Database 面板，打开 `sql/schema.sql` 全选执行。

执行完你会得到：
- 数据库 `smart_ticket`
- 表 `ticket`（工单）、`sys_user`（用户）
- 管理员账号：**admin / 123456**
- 3 条测试工单

### 第 3 步：改数据库密码

打开 `src/main/resources/application.yml`，把 `password: 123456` 改成**你自己 MySQL 的 root 密码**。

### 第 4 步：启动 Java 项目

用 IDEA 打开 `smart-ticket` 文件夹，等 Maven 依赖下载完（右下角进度条走完），
然后右键 `SmartTicketApplication.java` → **Run**。

看到这样的输出就成功了：

```
====================================================
  智能客服工单系统启动成功！
  接口地址: http://localhost:8080
====================================================
```

### 第 5 步（可选）：启动 Python AI 服务

第 1-4 周可以先跳过，等 Java 部分跑通了再回来做。

```powershell
cd ai-service
pip install -r requirements.txt
python -m uvicorn main:app --reload --port 8000
```

启动后打开 <http://localhost:8000/docs>，可以在网页上直接点按钮测试 AI 接口。

---

## 四、接口清单

除了登录接口，其它都需要在请求头带上 `Authorization: Bearer <token>`。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/login` | 登录，返回 token |
| GET | `/api/auth/info` | 获取当前登录用户 |
| GET | `/api/ticket/page?pageNum=1&pageSize=10&status=&keyword=` | 分页查询 |
| GET | `/api/ticket/{id}` | 工单详情 |
| POST | `/api/ticket` | 新建工单 |
| PUT | `/api/ticket/{id}/status?status=1` | 修改状态 |
| DELETE | `/api/ticket/{id}` | 删除（逻辑删除） |
| GET | `/api/ticket/stats` | 首页统计（走 Redis 缓存） |
| GET | `/health` | AI 服务健康检查（Python） |
| POST | `/ai/classify` | 工单自动分类（Python） |
| POST | `/ai/reply` | 生成回复草稿（Python） |

### 用 curl 快速验证

```bash
# 1. 登录，拿到 token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"123456\"}"

# 2. 用 token 查工单列表（把 <token> 换成上一步返回的）
curl http://localhost:8080/api/ticket/page \
  -H "Authorization: Bearer <token>"

# 3. 查统计（第一次走数据库，第二次命中 Redis 缓存，控制台会打印"统计数据命中缓存"）
curl http://localhost:8080/api/ticket/stats \
  -H "Authorization: Bearer <token>"
```

---

## 五、项目结构

```
smart-ticket/
├── pom.xml                          Maven 依赖
├── sql/
│   └── schema.sql                   建库建表脚本
├── ai-service/                      Python AI 服务（第 5-6 周做）
│   ├── main.py                      FastAPI 主程序
│   └── requirements.txt
└── src/main/
    ├── java/com/smartticket/
    │   ├── SmartTicketApplication.java   启动类
    │   ├── common/                       通用组件
    │   │   ├── Result.java               统一返回格式
    │   │   ├── BizException.java         业务异常
    │   │   └── GlobalExceptionHandler.java  全局异常处理
    │   ├── config/                       配置类
    │   │   ├── MybatisPlusConfig.java    分页插件
    │   │   ├── RedisConfig.java          Redis JSON 序列化
    │   │   └── WebConfig.java            拦截器 + 跨域
    │   ├── interceptor/
    │   │   └── JwtInterceptor.java       登录校验拦截器
    │   ├── util/
    │   │   └── JwtUtil.java              token 生成与解析
    │   ├── entity/                       数据库实体
    │   │   ├── Ticket.java
    │   │   └── SysUser.java
    │   ├── mapper/                       数据访问层
    │   │   ├── TicketMapper.java
    │   │   └── SysUserMapper.java
    │   ├── service/                      业务层
    │   │   ├── TicketService.java
    │   │   ├── SysUserService.java
    │   │   └── impl/
    │   ├── dto/
    │   │   └── LoginRequest.java
    │   └── controller/                   接口层
    │       ├── TicketController.java
    │       └── AuthController.java
    └── resources/
        └── application.yml               配置文件
```

---

## 六、踩坑记录（面试可以讲，显得你真做过）

### 坑 1：MyBatis-Plus 3.5.9 之后拆了模块

网上 90% 的教程写的是：

```java
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;   // 老版本
```

但从 **3.5.9** 开始，`IService` / `ServiceImpl` 搬到了新包：

```java
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;     // 新版本
```

而且 `PaginationInnerInterceptor`（分页拦截器）被移出了 `mybatis-plus-extension`，
必须额外引入 `mybatis-plus-jsqlparser` 依赖，否则分页配置类直接编译不过。

**跟着老教程做，这一步一定会卡住。**

### 坑 2：Maven 本地仓库权限

报错 `java.nio.file.AccessDeniedException: D:\IDEA\apache-maven-3.9.4\mvn_repo\...`
说明 Maven 没有权限写本地仓库。检查 `D:\IDEA\apache-maven-3.9.4\conf\settings.xml`
里的 `localRepository` 配置，或者用管理员身份运行。

### 坑 3：Redis 没启动

报错 `Unable to connect to Redis`。执行 `net start Redis`。
本项目对 Redis 做了降级处理，Redis 挂了接口也能用，但缓存就不生效了。

### 坑 4：JDK 25 太新

你的 JDK 是 25，Spring Boot 必须用 **3.5.x** 以上。网上 Spring Boot 2.x 的教程
在你机器上会报各种奇怪的错，别浪费时间。

---

## 七、8 周开发计划

> 📌 **两份配套文档：**
> - **[`每日计划.md`](./每日计划.md)** —— 按真实日期排的日程表，每天打开看「今天干什么」
> - **[`TASKS.md`](./TASKS.md)** —— 每个任务的详细做法、完成标准、对应面试题

| 周次 | 做什么 | 状态 |
|---|---|---|
| W1 | 环境搭建、建库建表、跑通项目启动 | ⬜ |
| W2 | 读懂现有代码：Entity → Mapper → Service → Controller 一条链路 | ⬜ |
| W3 | 自己动手加功能：工单评论 / 附件上传 / 操作日志 | ⬜ |
| W4 | Vue3 前端：登录页 + 工单列表 + 新建工单 | ⬜ |
| W5 | Python AI 服务：跑通 `/ai/classify`，并让 Java 调用它 | ⬜ |
| W6 | 接真实大模型（DeepSeek），做 RAG 相似工单检索 | ⬜ |
| W7 | 部署到服务器，拿到公网可访问的链接 | ⬜ |
| W8 | 简历定稿 + 大规模投递 + 面试复盘 | ⬜ |

---

## 八、下一步可以加的功能（简历加分项）

- [ ] 工单评论 / 回复记录表
- [ ] 附件上传（本地存储 → 对象存储 OSS/MinIO）
- [ ] 操作日志（AOP 切面记录谁改了什么）
- [ ] RabbitMQ 异步发通知（工单创建后发短信/邮件）
- [ ] 用 DeepSeek 做真实分类，记录准确率
- [ ] 向量检索历史相似工单（Chroma / Milvus）
- [ ] Docker Compose 一键启动整个环境
- [ ] Vue3 + Element Plus 前端
- [ ] 部署上线（宝塔 / 阿里云学生机）
