```markdown
# 登录功能文档

## 功能概述

基于 JWT 的多角色登录认证系统，支持医生（roleType=2）和管理员（roleType=3）身份验证。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + TypeScript + Element Plus + Axios |
| 后端 | Spring Boot + MyBatis-Plus + JWT |
| 数据库 | PostgreSQL |

## 文件结构

```
前端
├── src/views/LoginView.vue          # 登录页面
├── src/router/index.ts              # 路由守卫配置

后端
├── com/cloudbrainmed/auth/controller/AuthController.java
├── com/cloudbrainmed/auth/service/AuthService.java
├── com/cloudbrainmed/auth/service/impl/AuthServiceImpl.java
├── com/cloudbrainmed/auth/dto/LoginDto.java
├── com/cloudbrainmed/auth/mapper/AdminMapper.java
├── com/cloudbrainmed/auth/mapper/DoctorMapper.java
├── com/cloudbrainmed/auth/entity/Admin.java
└── com/cloudbrainmed/auth/entity/Doctor.java
```

## 接口说明

### 登录请求

```
POST /auth-service/login
Content-Type: application/json
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| phone | string | 是 | 手机号 |
| password | string | 是 | 密码 |
| roleType | int | 是 | 2=医生，3=管理员 |

**请求示例：**

```json
{
  "phone": "13800000000",
  "password": "123456",
  "roleType": 3
}
```

**响应示例：**

```json
{
  "code": 200,
  "data": "eyJhbGciOiJIUzI1NiIs...",
  "msg": "success"
}
```

## 前端代理配置

`vite.config.ts` 中将 `/auth-service` 请求代理到认证服务（端口8000）：

```js
export default defineConfig({
  server: {
    proxy: {
      '/auth-service': {
        target: 'http://localhost:8000',
        changeOrigin: true,
      },
      '/ai-service': {
        target: 'http://localhost:8001',
        changeOrigin: true,
      }
    }
  }
})
```

## 数据库表结构

### admin 表（管理员）

```sql
CREATE TABLE admin (
    admin_id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(20) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100),
    avatar VARCHAR(255),
    gender VARCHAR(10),
    email VARCHAR(100),
    position VARCHAR(100),
    status INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0
);
```

### doctor 表（医生）

```sql
CREATE TABLE doctor (
    doctor_id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(20) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100),
    avatar VARCHAR(255),
    gender VARCHAR(10),
    email VARCHAR(100),
    department VARCHAR(100),
    title VARCHAR(100),
    status INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0
);
```

## 服务端口

| 服务 | 端口 |
|------|------|
| 前端（Vite） | 5173 |
| 认证服务（AuthService） | 8002 |
| AI服务（AiService） | 8001 |
| 医生服务（DoctorService） | 8003 |
| 患者服务（PatientService） | 8004 |

## 启动流程

### 1. 准备数据库

```bash
# 启动 PostgreSQL
# 执行上述建表 SQL
```

### 2. 启动后端认证服务

```bash
cd AuthService
mvn spring-boot:run
```

### 3. 启动前端

```bash
npm install
npm run dev
```

### 4. 访问登录页

```
http://localhost:5173/login
```

## 测试账号

| 角色 | 手机号         | 密码 | roleType |
|------|-------------|------|----------|
| 医生 | 11111111111 | 123456 | 2 |

## 常见问题及解决方案

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 404 Not Found | AuthService未启动或端口错误 | 检查8002端口服务是否运行 |
| 500 column "avatar" does not exist | 数据库表缺少avatar字段 | 执行 `ALTER TABLE admin ADD COLUMN avatar VARCHAR(255);` |
| 登录后无法跳转 | token未正确存储 | 检查sessionStorage是否存储成功 |
| CORS跨域错误 | 代理配置未生效 | 重启Vite开发服务器 |