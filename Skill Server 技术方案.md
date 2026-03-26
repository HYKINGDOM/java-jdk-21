# Skill Server 技术方案 v2

## 1. 项目定位

Skill Server 是一个面向团队内部知识资产管理的 Skill 注册与分发服务。
当前版本服务端已统一切换为：

- `Java 21`
- `Spring Boot 3.3.x`
- `Spring MVC + Spring Security + Spring Data JPA`

项目目标是把 Skill 作为“目录级版本单元、`SKILL.md` 作为检索单元”进行统一管理，支持私有 Skill 创建、文件上传、在线编辑、Git 仓库导入、版本时间线、搜索索引、收藏、通知、下载、审计和基础定时清理。

---

## 2. 技术栈替换结论

### 2.1 原技术栈到 Java 技术栈映射

| 原方案 | Java 方案 |
| --- | --- |
| FastAPI | Spring Boot + Spring MVC |
| SQLAlchemy / SQLModel | Spring Data JPA + Hibernate |
| Pydantic | Jackson + Jakarta Bean Validation |
| APScheduler | Spring Scheduling |
| GitPython / Dulwich | JGit |
| Whoosh | Apache Lucene |
| markdown-it-py | CommonMark Java |
| 本地脚本式任务状态管理 | Spring Service + JPA Job Table |
| Python 单体应用 | Java 21 单体服务 |

### 2.2 当前实际依赖

- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-security`
- `spring-boot-starter-validation`
- `spring-boot-starter-actuator`
- `H2 / PostgreSQL`
- `JGit`
- `Apache Lucene`
- `CommonMark`

---

## 3. 核心设计原则

### 3.1 管理单位

- Skill 的版本管理单位是“整个目录”
- Skill 的检索单位是 `SKILL.md`
- 时间线展示单位是一次目录级提交产生的文件变更集合

### 3.2 分层规则

- `Controller` 负责 HTTP 入口
- `Service` 负责业务编排
- `Repository` 负责数据访问
- 文件系统工作区由 `WorkspaceService` 统一管理
- 搜索索引由 `SearchIndexService` 统一维护

### 3.3 一致性约束

- 先落库 Skill 元数据，再维护当前目录快照
- 每次目录变更都生成新的 revision 记录
- 当 `SKILL.md` 内容变化时才重建搜索索引
- 删除采用软删除，定时任务做物理清理

---

## 4. 系统架构

### 4.1 总体架构

```text
Client
  |
  v
Spring MVC Controllers
  |
  v
Service Layer
  |-- SkillService
  |-- EditSessionService
  |-- GitRepoService
  |-- FavoriteService
  |-- NotificationService
  |-- AuditLogService
  |-- SearchIndexService
  |-- WorkspaceService
  |
  +--> Spring Data JPA / Hibernate --> H2 or PostgreSQL
  |
  +--> Local Workspace / Package / Mirror / Index Directories
```

### 4.2 已落地的核心能力

- 私有 Skill 页面创建
- 单个 `SKILL.md` 上传创建
- 压缩包目录上传创建
- 私有 Skill 编辑锁
- 私有 Skill 在线更新
- 私有 Skill 软删除
- Git 仓库导入
- Git 仓库手动同步
- Skill 列表 / 详情 / 树 / 时间线 / 版本详情 / 下载
- 收藏 / 通知
- 审计日志入库
- Lucene 全文检索
- 定时过期编辑锁清理
- 定时软删除物理清理

---

## 5. 项目目录

```text
src/main/java/com/skillserver
├── SkillServerApplication.java
├── common
│   ├── entity
│   ├── exception
│   └── util
├── config
├── domain
│   ├── entity
│   └── enums
├── dto
│   ├── common
│   └── skill
├── repository
├── security
├── service
│   ├── SkillService.java
│   ├── GitRepoService.java
│   ├── EditSessionService.java
│   ├── SearchIndexService.java
│   ├── WorkspaceService.java
│   └── ...
└── web
    ├── SkillController.java
    ├── PrivateSkillController.java
    ├── RepoController.java
    ├── EditSessionController.java
    ├── FavoriteController.java
    ├── NotificationController.java
    └── TraceIdFilter.java
```

---

## 6. 核心业务对象

### 6.1 主表

- `users`
- `repo_sources`
- `skills`
- `skill_current_docs`
- `skill_revisions`
- `skill_revision_files`
- `skill_files_current`
- `skill_favorites`
- `notifications`
- `skill_downloads`
- `sync_jobs`
- `index_jobs`
- `audit_logs`
- `edit_sessions`
- `resource_roles`

### 6.2 关键字段说明

#### `skills`

- `skill_uid`: Skill 全局唯一标识
- `source_type`: `PRIVATE` / `GIT`
- `current_revision`: 当前目录版本号
- `current_tree_fingerprint`: 当前目录树指纹
- `current_skill_md_fingerprint`: 当前 `SKILL.md` 指纹
- `index_status`: `PENDING` / `INDEXED` / `INDEX_FAILED`
- `sync_status`: `IDLE` / `SYNCED` / `SYNC_FAILED`
- `status`: `ACTIVE` / `DELETED_PENDING_PURGE` / `PURGED`

#### `skill_current_docs`

- 保存当前检索文档快照
- 存储标题、摘要、标签、Markdown 正文、纯文本正文、内容哈希

#### `skill_revisions`

- 保存每次目录提交的版本历史
- 记录父版本、变更范围、文件变更统计、目录指纹和摘要

#### `skill_files_current`

- 保存当前目录树快照
- 包含文件路径、预览模式、大小、MIME、SHA256

#### `repo_sources`

- 保存 Git 来源仓库
- 记录 URL、分支、同步状态、最后 commit、最后 revision

---

## 7. 文件系统设计

### 7.1 工作区

- `workspace/private-skills`: 私有 Skill 当前目录
- `workspace/mirrors`: Git 仓库镜像目录
- `workspace/tmp`: 上传解压和编辑临时目录
- `workspace/packages`: 下载包输出目录
- `data/index`: Lucene 索引目录

### 7.2 目录规则

- 私有 Skill 路径：`workspace/private-skills/{skillUid}`
- Git 仓库镜像路径：`workspace/mirrors/repo-{repoId}`
- Git Skill 当前目录：镜像目录下的 `relativeDir`

### 7.3 安全控制

- ZIP 解压时校验路径穿越
- 写入文本文件时统一做相对路径规范化
- 默认忽略 Git 内部 `.git` 元数据目录

---

## 8. 搜索设计

### 8.1 实现方式

- 使用 `Apache Lucene`
- 索引字段：
  - `skillUid`
  - `title`
  - `summary`
  - `body`
  - `tags`
  - `sourceType`
  - `status`
  - `updatedAt`

### 8.2 索引策略

- Skill 创建时建立索引
- 私有 Skill 编辑时：
  - 如果 `SKILL.md` 改变，执行 `update`
  - 如果只改其他文件，不重建索引
- 删除 Skill 时移除索引文档

### 8.3 查询策略

- 关键字搜索走 Lucene
- 来源类型、标签、权限在业务层二次过滤
- 默认按更新时间排序
- 当存在关键字查询且未指定排序时按相关度排序

---

## 9. 权限设计

### 9.1 系统角色

- `ADMIN`
- `MEMBER`

### 9.2 资源角色

- `OWNER`
- `MAINTAINER`
- `EDITOR`
- `VIEWER`

### 9.3 当前实现规则

- `ADMIN` 拥有全部权限
- Git Skill 对所有登录用户可读
- 私有 Skill 默认仅创建者拥有 `OWNER`
- `OWNER / MAINTAINER / EDITOR` 可编辑
- `OWNER / MAINTAINER` 可删除

---

## 10. 核心流程

### 10.1 私有 Skill 创建

1. 接收页面 JSON、`SKILL.md` 文件或 ZIP 包
2. 写入临时目录
3. 校验并解析 `SKILL.md`
4. 计算目录树指纹和 `SKILL.md` 指纹
5. 移动到正式工作区
6. 写入 `skills`
7. 写入 `skill_current_docs`
8. 写入 `skill_files_current`
9. 生成首个 `skill_revisions`
10. 建立 Lucene 索引

### 10.2 私有 Skill 编辑

1. 申请编辑锁
2. 校验 `lockToken + baseRevision`
3. 复制当前目录到临时目录
4. 应用文件增删改
5. 重新计算指纹并比较变更
6. 生成新 revision
7. 刷新当前目录快照
8. 如 `SKILL.md` 改变则重建索引

### 10.3 Git 仓库导入

1. 创建 `repo_sources`
2. 本地路径场景复制到 mirror，远端仓库场景走 JGit
3. 扫描所有包含 `SKILL.md` 的目录
4. 为每个 Skill 创建或更新 `skills`
5. 建立当前目录快照、版本记录和索引
6. 更新 `sync_jobs`

---

## 11. API 设计

### 11.1 私有 Skill

- `POST /skills/private/create`
- `POST /skills/private/upload-skill-md`
- `POST /skills/private/upload-folder`
- `PUT /skills/private/{skillUid}`
- `DELETE /skills/private/{skillUid}`

### 11.2 编辑锁

- `POST /skills/private/{skillUid}/edit-session`
- `POST /skills/private/{skillUid}/edit-session/heartbeat`
- `DELETE /skills/private/{skillUid}/edit-session?lockToken=...`

### 11.3 Git 仓库

- `POST /repos/import`
- `POST /repos/{repoId}/sync`
- `DELETE /repos/{repoId}`

### 11.4 通用能力

- `GET /skills`
- `GET /skills/{skillUid}`
- `GET /skills/{skillUid}/tree`
- `GET /skills/{skillUid}/timeline`
- `GET /skills/{skillUid}/timeline/{revision}`
- `GET /skills/{skillUid}/download`
- `POST /skills/{skillUid}/favorite`
- `DELETE /skills/{skillUid}/favorite`
- `GET /notifications`
- `POST /notifications/{id}/read`

---

## 12. 配置设计

### 12.1 `application.yml`

默认提供：

- H2 文件数据库
- 虚拟线程开启
- JPA 自动建表
- 上传大小限制
- Actuator 健康检查
- 工作区目录配置
- 清理任务配置

### 12.2 `application-postgres.yml`

提供 PostgreSQL 切换能力：

- `POSTGRES_URL`
- `POSTGRES_USERNAME`
- `POSTGRES_PASSWORD`

---

## 13. 安全与审计

### 13.1 认证

- 当前实现使用 `Spring Security HTTP Basic`
- 初始化账户：
  - `admin / admin123`
  - `member / member123`

### 13.2 审计

以下操作会写入 `audit_logs`：

- Skill 创建
- Skill 更新
- Skill 删除
- 仓库导入
- 仓库同步
- 仓库删除
- 编辑锁申请

### 13.3 链路跟踪

- 通过 `TraceIdFilter` 注入 `X-Trace-Id`
- 审计日志记录 traceId

---

## 14. 定时任务

### 14.1 编辑锁过期任务

- 每分钟扫描一次
- 将过期 `ACTIVE` 锁改为 `EXPIRED`

### 14.2 删除清理任务

- 根据 cron 定时执行
- 物理删除私有 Skill 工作区
- 删除当前文档快照和当前目录快照
- 状态推进为 `PURGED`

---

## 15. 构建与运行

### 15.1 构建

```bash
mvn clean package
```

### 15.2 运行

```bash
mvn spring-boot:run
```

### 15.3 PostgreSQL 运行

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

---

## 16. 测试说明

当前仓库已包含并通过：

- Spring Boot 上下文启动测试
- 私有 Skill 创建 / 编辑 / 搜索 / 通知集成测试
- 本地 Git 仓库导入与检索集成测试

执行命令：

```bash
mvn test
```

---

## 17. 当前实现状态

### 17.1 已完成

- Java 21 + Spring Boot 单体服务端
- 领域模型与数据库表
- REST API
- 文件工作区
- 版本时间线
- Lucene 搜索
- JGit 导入
- 收藏 / 通知 / 下载 / 审计
- 定时清理
- 集成测试

### 17.2 可继续增强

- 资源角色分配管理接口
- 更强的 Git 认证方式
- 向量检索
- HTML 预览沙箱
- 对象存储替代本地文件系统
- 更细粒度的后台运维命令

---

## 18. 最终结论

本方案服务端已从原先的 Python/FastAPI 技术思路，完整切换为 `Java 21 + Spring Boot` 实现。
当前仓库中的代码、构建配置、数据库模型、API、搜索、Git 集成与文档说明已保持一致，可直接作为 Skill Server 的 Java 版服务端基线。
