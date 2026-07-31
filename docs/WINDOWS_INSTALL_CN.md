# ABMS Windows 11 安装指南

你当前的检查结果表示：

- Git：未安装
- Node.js：未安装
- npm：未安装，安装 Node.js 后会一起安装
- Java：已安装 Java 8，但 ABMS 需要 Java 21
- Maven：未安装
- Docker：未安装

请按下面顺序安装。

## 1. 安装 Git

打开：

```text
https://git-scm.com/download/win
```

下载并安装 Git for Windows。安装时保持默认选项即可。

验证：

```powershell
git --version
```

## 2. 安装 Node.js LTS

打开：

```text
https://nodejs.org/en/download
```

选择 Windows Installer，安装 LTS 版本。

验证：

```powershell
node --version
npm --version
```

## 3. 安装 Java 21

打开：

```text
https://adoptium.net/temurin/releases/?version=21
```

选择：

```text
Operating System: Windows
Architecture: x64
Package Type: JDK
Version: 21
```

安装完成后，重新打开 PowerShell。

验证：

```powershell
java -version
```

正确结果应该显示 `21`，不是 `1.8`。

## 4. 安装 Maven

打开：

```text
https://maven.apache.org/download.cgi
```

下载 Binary zip archive，例如 `apache-maven-3.x.x-bin.zip`。

解压到一个固定目录，例如：

```text
C:\Tools\apache-maven
```

然后把 Maven 的 `bin` 目录加入 Windows Path，例如：

```text
C:\Tools\apache-maven\bin
```

验证：

```powershell
mvn --version
```

## 5. 安装 Docker Desktop

打开：

```text
https://www.docker.com/products/docker-desktop/
```

安装 Docker Desktop。安装过程中如果提示启用 WSL 2，请允许。

安装完成后启动 Docker Desktop，等待左下角显示 Docker 正在运行。

验证：

```powershell
docker --version
docker compose version
```

## 6. 重新检查环境

关闭当前 PowerShell，重新打开一个新的 PowerShell。

进入项目目录：

```powershell
cd "C:\Users\black\Documents\Hello World\abms"
```

运行：

```powershell
.\scripts\check-env.cmd
```

如果最后显示：

```text
All required tools are ready.
```

就可以继续启动项目。

## 7. 启动 ABMS 第一阶段项目

打开三个 PowerShell 窗口，都进入：

```powershell
cd "C:\Users\black\Documents\Hello World\abms"
```

第一个窗口启动数据库：

```powershell
.\scripts\start-database.cmd
```

第二个窗口启动后端：

```powershell
.\scripts\start-backend.cmd
```

第三个窗口启动前端：

```powershell
.\scripts\start-frontend.cmd
```

最后打开浏览器：

```text
http://localhost:4200
```

应该看到：

```text
Hello ABA
```
