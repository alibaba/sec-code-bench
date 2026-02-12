# =========================================================================
# Stage 1: Base Image and Environment Setup
# =========================================================================
# 使用一个包含基本操作系统的 slim Python 镜像，这是一个很好的平衡点
# bullseye 是 Debian 11 的代号，指定版本可以确保构建的可复现性
FROM python:3.11-slim-bullseye

# 设置环境变量，避免交互式提示，并让 Python 日志立即输出
ENV DEBIAN_FRONTEND=noninteractive \
    PYTHONUNBUFFERED=1

# =========================================================================
# Install System Dependencies: JDK and Maven
# =========================================================================
# 在一个 RUN 指令中完成所有 apt 操作，以减小镜像层数
# 1. 更新包列表
# 2. 安装 OpenJDK 8 (一个常见的LTS版本) 和 Maven
# 3. 清理 apt 缓存，减小镜像体积
RUN apt-get update && \
    apt-get install -y --no-install-recommends openjdk-17-jdk maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 设置 JAVA_HOME 环境变量，Maven 和其他 Java 工具会用到它
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# 将工作目录设置为 /app
WORKDIR /app

# =========================================================================
# Install Python Dependencies (Leveraging Layer Caching)
# =========================================================================
# 1. 首先只复制 requirements.txt 文件
# 2. 安装依赖
# 这样做的好处是：只要 requirements.txt 没有变化，Docker 就会使用缓存的层，
# 无需在每次代码变更时都重新安装所有 Python 包，极大地加快了构建速度。
COPY requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt

# =========================================================================
# Pre-cache Maven Dependencies (Huge Performance Boost)
# =========================================================================
# 和上面的 Python 技巧类似，我们先预热 Maven 缓存。
# 1. 只复制 pom.xml 文件
# 2. 运行 `dependency:go-offline` 命令，这会下载 pom.xml 中定义的所有依赖
#    并存入镜像内的 .m2 目录。这样，在容器运行时执行 `mvn test` 会快得多，
#    因为它不需要再从网上下载依赖。

COPY . .

RUN bash scripts/install_java_test_deps.sh || echo "Warning: Some Maven dependencies may not be pre-cached"

# =========================================================================
# Expose Port and Define Run Command
# =========================================================================
# 声明容器将监听 8000 端口
EXPOSE 8000

# 容器启动时执行的命令
# 使用 0.0.0.0 作为主机，使其可以从外部访问
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"]
