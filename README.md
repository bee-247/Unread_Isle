# 未读岛（Unread Isle）

未读岛是一个面向知识创作、发现与交流的社区项目。用户可以发布“知文”，通过首页 Feed 和搜索发现内容，并使用点赞、收藏、关注及 AI 问答等功能完成从阅读到沉淀的完整流程。

项目采用前后端分离架构，并通过 Docker Compose 编排应用与基础设施，适合本地开发、功能演示和二次开发。

## 核心功能

- 用户注册、登录和 JWT 双令牌认证
- 知文草稿、Markdown 正文、图片上传与发布
- 首页 Feed、个人主页和内容详情页
- 点赞、收藏、关注、粉丝与用户计数
- Elasticsearch 全文搜索与内容索引
- AI 自动摘要和基于单篇知文的 RAG 流式问答
- 阿里云 OSS 文件直传与私有资源临时签名读取
- Redis、Caffeine 多级缓存及发布后 Feed 实时失效
- Kafka、Canal 与 Outbox 驱动的异步数据同步

## 技术架构

### 前端

- React 18
- TypeScript
- Vite
- React Router
- React Markdown
- Nginx

### 后端

- Java 21
- Spring Boot 3
- Spring Security、JWT
- Spring AI
- MyBatis
- MySQL
- Redis、Redisson、Caffeine
- Kafka、Canal
- Elasticsearch
- 阿里云 OSS

### 服务关系

```text
浏览器
  └── Frontend / Nginx :8088
        └── Backend / Spring Boot :8080
              ├── MySQL
              ├── Redis
              ├── Kafka + Canal
              ├── Elasticsearch
              ├── 阿里云 OSS
              └── AI 模型服务
```

## 项目结构

```text
Unread_Isle/
├── docker-compose.yml                 # 全部服务的 Docker Compose 编排
├── README.md                          # 项目说明
├── Unread_Isle_Backend/               # Spring Boot 后端
│   ├── db/schema.sql                  # 数据库初始化脚本
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
└── Unread_Isle_Frontend/              # React 前端
    ├── Dockerfile
    ├── nginx.conf
    ├── package.json
    └── src/
```

## 一键启动项目

请先安装并启动 Docker Desktop，然后进入项目根目录执行以下命令。

### 启动

在后台构建并启动前端、后端及全部基础服务：

```bash
docker compose --env-file .env up -d --build
```

启动完成后访问 <http://localhost:8088>。

### 关闭

停止并删除项目容器和网络，同时保留数据库等持久化数据：

```bash
docker compose --env-file .env down
```

不要随意添加 `-v` 参数，否则会同时删除数据库及其他 Docker 数据卷。

## 注意事项

- `.env`、JWT 私钥及云服务密钥属于敏感信息，不应提交到 Git。
- OSS Bucket 可以保持私有，后端会为允许访问的文件生成短期签名地址。
- `docker compose down` 不会删除持久化数据；`docker compose down -v` 会删除。
- Canal 使用 MySQL binlog 完成增量同步，当前 Compose 配置主要面向本地开发环境。
- AI 摘要、Embedding、RAG 与 OSS 功能依赖对应外部服务可用。
