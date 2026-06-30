# CloudBrainMed——docker使用说明

# Docker 微服务开发与部署指南



> 本文档面向团队开发人员，旨在统一 Docker 使用规范，打通从“本地编码”到“容器化部署”的全链路。
> 
> 



---



## 1\. 核心思想：两阶段工作流



微服务项目中，Docker 的使用分为两个截然不同的阶段，请大家严格区分：



|阶段|目标|微服务代码运行位置|中间件（Nacos/MySQL/Redis）运行位置|
|---|---|---|---|
|**阶段一：日常开发**|快速编码、调试、单元测试|**本地 IDEA**（运行启动类）|**Docker 容器**（固定环境）|
|**阶段二：集成/部署**|系统联调、测试上线、交付运维|**Docker 容器**（打成镜像）|**Docker 容器**（同上）|



---



## 2\. 阶段一：日常开发（推荐做法）



### 2\.1 中间件容器化（一次性配置）

编写 `docker-compose.yml`，**仅包含** Nacos、MySQL、Redis 等中间件，并做好端口映射，供本地代码访问。



```YAML
# docker-compose-infra.yml（仅中间件）
services:
  nacos:
    image: nacos/nacos-server:latest
    container_name: nacos
    ports:
      - "8848:8848"          # 宿主机端口 : 容器内端口（映射）
    environment:
      - MODE=standalone

  mysql:
    image: mysql:8.0
    container_name: mysql
    ports:
      - "3306:3306"
    environment:
      - MYSQL_ROOT_PASSWORD=123456

  redis:
    image: redis:7.0
    container_name: redis
    ports:
      - "6379:6379"
```



**启动命令：**

```Bash
docker compose -f docker-compose-infra.yml up -d
```



### 2\.2 本地微服务配置

在本地微服务的 `application.yml` 中，所有中间件地址均指向 **`localhost`**（因为已做端口映射）。



```YAML
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848   # 连接 Docker 内的 Nacos
  datasource:
    url: jdbc:mysql://localhost:3306/user_db
    username: root
    password: 123456
  redis:
    host: localhost
    port: 6379
```



### 2\.3 日常启动流程

1. 确保 Docker 中间件容器已启动（执行上面的 `docker compose up -d`）。

2. 在 IDEA 中，直接右键微服务的 `main` 方法，点击 `Run`。

3. 修改代码后，点击 IDEA 的 `Rerun` 按钮或利用 `spring-boot-devtools` 自动重启，**全程无需重新构建镜像**（秒级生效）。

    

---



## 3\. 阶段二：最终部署（打包镜像）



当完成功能开发，准备进行集成测试或交付时，**将所有服务**（含微服务）容器化。



### 3\.1 编写 Dockerfile（每个微服务都需要）

在每个微服务模块根目录下创建 `Dockerfile`：



```Dockerfile
FROM openjdk:17-jdk-slim
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```



### 3\.2 编写最终部署用的 `docker-compose.yml`

此文件包含**中间件 \+ 所有微服务**。此时，微服务之间的访问**不再使用 ****`localhost`**，而是使用容器**服务名**（如 `nacos`、`mysql`）。



```YAML
# docker-compose-full.yml（完整部署）
services:
  # 中间件
  nacos:
    image: nacos/nacos-server:latest
    ports:
      - "8848:8848"
  mysql:
    image: mysql:8.0
    ports:
      - "3306:3306"
  
  # 业务微服务（构建本地代码）
  user-service:
    build: ./user-service          # 读取该目录下的 Dockerfile
    ports:
      - "8081:8081"
    environment:
      - SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR=nacos:8848  # 关键：用服务名
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/user_db

  order-service:
    build: ./order-service
    ports:
      - "8082:8082"
    environment:
      - SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR=nacos:8848
```



### 3\.3 构建与启动

```Bash
# 一键构建镜像、启动所有容器
docker compose up -d --build
```



---



## 4\. 常见问题与避坑指南



### 4\.1 配置文件管理（多环境隔离）

建议准备两份配置文件，避免频繁修改：



- `application-dev.yml`：配置 `localhost`，用于本地开发。

- `application-prod.yml`：配置服务名（如 `nacos`），用于容器部署。

    

启动时指定环境：

```Bash
# 本地开发
java -jar app.jar --spring.profiles.active=dev

# Docker 部署（在 docker-compose 中配置）
environment:
  - SPRING_PROFILES_ACTIVE=prod
```



### 4\.2 重新构建镜像的场景

|操作内容|是否需要重新构建镜像？|正确做法|
|---|---|---|
|修改代码逻辑（Java 类）|**✅ 需要**|`mvn package` \+ `docker compose build`|
|修改依赖包（pom\.xml）|**✅ 需要**|同上|
|修改数据库连接、日志级别等配置|**❌ 不需要**|改 `docker-compose.yml` 环境变量，重启容器即可|



### 4\.3 强制重新构建（忽略缓存）

```Bash
docker compose build --no-cache
```



---



## 5\. 核心命令速查表



|需求场景|命令|
|---|---|
|启动中间件（开发用）|`docker compose -f docker-compose-infra.yml up -d`|
|查看容器日志|`docker compose logs -f [服务名]`|
|停止所有容器|`docker compose down`|
|构建并启动完整环境（部署用）|`docker compose -f docker-compose-full.yml up -d --build`|
|强制重建镜像（无缓存）|`docker compose build --no-cache`|
|重启单个容器|`docker restart [容器名]`|



---



## 6\. 总结



1. **开发时**：Docker 跑中间件，IDE 跑代码，利用端口映射（`localhost`）互联，享受秒级重启。

2. **部署时**：Docker 跑所有服务（含代码），利用容器网络（服务名）互联，实现环境一致性。

3. **配置分离**：利用 `application-{profile}.yml` 和环境变量，做到本地与容器配置互不干扰。

---



## 7\.命令直接对比



|命令|含义|会不会重新构建镜像？|
|---|---|---|
|`docker compose up -d`|启动容器（如果已存在则直接启动）|❌ **不会**|
|`docker compose up -d --build`|先构建镜像，再启动容器|✅ **会**|



---



### 7\.1什么场景用哪个？



#### 场景一：第一次启动，或者中间件没变化



```Bash
docker compose up -d
```



- 你的镜像已经存在（比如 Nacos、MySQL 的镜像已经拉取过了）

- 你只是想启动容器，不需要改动任何东西

- **执行速度快**，几秒钟就起来了

    

#### 场景二：你修改了代码，要重新部署



```Bash
docker compose up -d --build
```



- 你改了 Java 代码或 Dockerfile

- 需要重新打包成新镜像

- 会先执行 `docker compose build`，再执行 `up -d`

    

---



#### 如果你只改了配置文件（环境变量）



比如你改了 `docker-compose.yml` 里的 `SPRING_DATASOURCE_URL`，**不需要重新构建镜像**，只需要：



```Bash
# 方法一：直接重启（推荐）
docker compose restart

# 方法二：重新创建容器（会读取新配置）
docker compose up -d
```



---

#### 一个常见误区



❌ **错误想法**：每次都要加 `--build` 才安心。



✅ **正确做法**：只有在代码变更时才加 `--build`，否则不加，省时间。



**举个例子：**

- 你改了 `application.yml` 里的日志级别 → 只用 `docker compose restart`，**1秒搞定**

- 你改了 Java 业务逻辑 → 用 `docker compose up -d --build`，可能要 **1分钟**

    

所以，`docker compose up -d` 和 `docker compose up -d --build` **不是同一个意思**，差一个 `--build` 就差了很多时间。



如果你拿不准，每次都用 `docker compose up -d` 就行，Docker 会提示你“镜像已存在，直接启动”。只有当你明确改了代码，才主动加上 `--build`。😊



