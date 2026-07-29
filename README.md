# 未读岛（Unread_Isle）

未读岛是一个知识获取与分享社区，支持发布“知文”、首页 Feed、搜索、点赞收藏、关注关系、个人主页，以及 AI 摘要和基于单篇知文的 RAG 问答。

## 技术栈

### 后端

- Java 21、Spring Boot、Spring Security
- MyBatis、MySQL
- Redis、Redisson、Caffeine
- Kafka、Canal
- Elasticsearch
- Spring AI、DeepSeek
- 阿里云 OSS

### 前端

- React 18
- TypeScript
- Vite

## 项目结构

```text
Unread_Isle/
├── docker-compose.yml                # 前端、后端及基础服务编排
├── .env.example                      # 环境变量模板
├── Unread_Isle_Backend/                 # Spring Boot 后端
│   ├── db/schema.sql                 # 数据库结构
│   └── src/main/resources/
│       └── application-local.yml     # 本地环境配置
└── Unread_Isle_Frontend/                # React 前端
```

## 主要功能

- 手机号或邮箱注册、登录及 JWT 双令牌认证
- 知文草稿、OSS 直传、发布、编辑和删除
- 首页 Feed 与多级缓存
- 点赞、收藏及高并发计数
- 关注、取关、关注列表和粉丝列表
- Elasticsearch 内容搜索与联想
- DeepSeek 自动生成摘要
- 基于 Elasticsearch 向量库的单篇知文 RAG 问答

## 一键启动项目

### 1. 环境要求

一键启动只需要安装 Docker Desktop。只有使用下文的开发模式时，才需要在
宿主机额外安装 JDK 21、Maven 3.9+、Node.js 20+ 和 npm。

检查环境：

```bash
docker version
docker compose version
```

### 2. 配置环境变量

在项目根目录复制环境变量模板：

```bash
cp .env.example .env
```

生成只保存在本机的 JWT 密钥：

```bash
mkdir -p .secrets/jwt
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 \
  -out .secrets/jwt/private.pem
openssl pkey -in .secrets/jwt/private.pem -pubout \
  -out .secrets/jwt/public.pem
```

编辑 `.env`，搜索 `CHANGE_ME`，填写以下参数：

```dotenv
# MySQL 和 Redis
MYSQL_PASSWORD=...
MYSQL_ROOT_PASSWORD=...
REDIS_PASSWORD=...

# AI
DEEPSEEK_API_KEY=...
OPENAI_API_KEY=...

# 阿里云 OSS
OSS_ENDPOINT=...
OSS_ACCESS_KEY_ID=...
OSS_ACCESS_KEY_SECRET=...
OSS_BUCKET=...
```

默认使用 OpenAI Embedding。如果使用其他兼容服务，还需要修改：

```dotenv
OPENAI_BASE_URL=...
EMBEDDING_MODEL=...
EMBEDDING_DIMENSIONS=...
```

`EMBEDDING_DIMENSIONS` 必须与所选模型的实际向量维度一致。

`.env` 包含敏感信息，已经加入 `.gitignore`，请勿提交。

### 3. 启动完整项目

在项目根目录运行：

```bash
docker compose --env-file .env up -d --build
```

该命令会启动：

- React 前端（Nginx）
- Spring Boot 后端
- MySQL
- Redis
- Kafka
- Elasticsearch
- Canal

查看状态：

```bash
docker compose --env-file .env ps
```

启动完成后访问：

```text
http://localhost:8088
```

前端 Nginx 会自动把 `/api` 请求转发到后端，通常不需要直接访问
后端的 `8080` 端口。

首次创建 MySQL 数据卷时，Compose 会自动执行：

```text
Unread_Isle_Backend/db/schema.sql
```

如果服务启动失败，可以查看日志：

```bash
docker compose logs -f mysql
docker compose logs -f redis
docker compose logs -f kafka
docker compose logs -f elasticsearch
docker compose logs -f canal
```

## 开发模式

一键 Docker 启动适合首次运行、联调和演示。需要前端热更新或后端调试时，
可以只用 Docker 启动基础服务，再分别启动前后端。

### 1. 只启动基础服务

```bash
docker compose --env-file .env up -d mysql redis kafka elasticsearch canal
```

### 2. 启动后端

在项目根目录加载环境变量：

```bash
set -a
source .env
set +a
```

启动 Spring Boot：

```bash
mvn -f Unread_Isle_Backend/pom.xml spring-boot:run
```

`.env` 已设置 `SPRING_PROFILES_ACTIVE=local`，后端会读取：

```text
Unread_Isle_Backend/src/main/resources/application-local.yml
```

后端默认地址：

```text
http://localhost:8080
```

检查健康状态：

```bash
curl http://localhost:8080/actuator/health
```

### 3. 启动前端

打开另一个终端：

```bash
cd Unread_Isle_Frontend
npm ci
npm run dev
```

访问：

```text
http://localhost:5173
```

开发环境中，Vite 会自动把 `/api` 请求代理到 `http://localhost:8080`。

## 停止项目

先停止前后端进程，再停止 Docker 服务：

```bash
docker compose down
```

如需同时删除 MySQL、Redis、Kafka 和 Elasticsearch 的本地数据：

```bash
docker compose down -v
```

> `docker compose down -v` 会永久删除本地服务数据，请谨慎使用。

## 注意事项

- 项目中的 JWT 密钥仅供本地开发，生产环境请重新生成并使用密钥管理服务。
- 本地 Canal 使用 MySQL root 账号读取 binlog，仅适合开发环境。
- 搜索索引使用 `ik_max_word` 和 `ik_smart`，完整中文搜索需要为 Elasticsearch 安装版本匹配的 IK 分词插件。
- AI 与 OSS 功能依赖有效的外部服务凭据。
