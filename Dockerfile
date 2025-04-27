# 第一阶段：执行构建
# Step 1/17 : 构建
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Step 2/17 :设置工作目录（与宿主机项目路径一致）
WORKDIR /app

# Step 3/17 :仅复制 Dockerfile文件
COPY Dockerfile .

# Step 4/17 :仅复制 pom.xml
COPY pom.xml .

# Step 5/17 :下载所有依赖（包括测试依赖和插件依赖）
RUN mvn dependency:resolve-plugins dependency:go-offline -B

# Step 6/17 :复制源代码
COPY src ./src

# Step 7/17 :执行构建
RUN mvn clean package -DskipTests


# 第二阶段：运行环境
# Step 8/17 : 执行运行环境
FROM eclipse-temurin:21-jre-alpine AS runtime

# Step 9/17 : 设置工作目录
WORKDIR /app

# Step 10/17 :创建非 root 用户组，提升安全性
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Step 11/17 : 创建非 root 用户
USER appuser

# Step 12/17 :复制构建产物
COPY --from=build /app/target/department-management-*.jar app.jar

# Step 13/17 :镜像元数据，联系信息
LABEL maintainer="aaa@gmail.com"

# Step 14/17 : 镜像元数据，版本标签
LABEL version="1.0"

# Step 15/17 :暴露端口
EXPOSE 80

# Step 16/17 :健康检查（可选，需应用暴露 /actuator/health）
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:80/actuator/health || exit 1

# Step 17/17 :ENTRYPOINT：指定非阻塞熵源，避免 SecureRandom 阻塞
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
