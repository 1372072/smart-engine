# SmartEngine 构建与独立集成验证

验证日期：2026-11-05。

## 结论

本地修复核心模块 Maven 坐标后，完整构建、测试、打包及安装成功。可以作为 JAR 组件集成到其他 Java 服务。这里验证了独立项目通过 Maven 仓库加载组件并执行最小流程；不代表所有业务场景或真实数据库环境均已验收。

源码：`https://github.com/1372072/smart-engine.git`，分支 `master`，基准提交 `753bfe0a9dc22e10c73da8c0db926072e4fd4264`。

## 环境和修复

- JDK：`D:\Documents\ChatGPT\.jdks\jdk-25.0.5.0.3`
- Maven：3.9.11
- 本地仓库：`D:\Documents\ChatGPT\.m2`（直接作为 repository 根目录，不附加 repository 子目录）
- 唯一生产构建配置修改：`core/pom.xml` 中 artifactId 从 `core` 改为 `smart-engine-core`，与扩展模块的依赖一致。未修改业务源代码，未推送远程。

原始源码的 `clean package` 在 storage-custom 模块失败，因为无法解析 `com.alibaba.smart.framework:smart-engine-core:4.0.0-boot4`。见根目录 `build-jdk25.log`。

## 实测结果

- `clean install`：10 个 reactor 模块全部 SUCCESS，产出 9 个 JAR 和父 POM，已安装到指定 Maven 仓库。
- 133 份 Surefire XML 报告合计：567 次测试执行，0 失败、0 错误、0 跳过。
- 独立消费项目 `standalone-consumer`：1 项测试通过。该项目不继承 SmartEngine 父 POM，也不在根 reactor 中；通过依赖坐标解析已安装 JAR。
- 独立测试检查核心类来源确实是 JAR，初始化引擎、部署 BPMN、启动流程，断言流程完成且有完成时间。
- 数据库相关现有测试使用 H2；本次没有连接真实 MySQL/PostgreSQL。
- 日志：根目录 `build-fixed.log`、`build-consumer.log`。

核心产物：`core/target/smart-engine-core-4.0.0-boot4.jar`。其他扩展产物在各模块 `target` 目录中。

## 复现命令（PowerShell，在项目根目录执行）

```powershell
$env:JAVA_HOME = 'D:\Documents\ChatGPT\.jdks\jdk-25.0.5.0.3'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn -B -ntp '-Dmaven.repo.local=D:\Documents\ChatGPT\.m2' clean install
mvn -B -ntp '-Dmaven.repo.local=D:\Documents\ChatGPT\.m2' -f verification/standalone-consumer/pom.xml clean verify
```

`install` 包含编译、测试和 package，并把父 POM 和模块产物安装到本地仓库。命令仅设置当前 PowerShell 会话的 JDK，不修改系统环境变量。

## 新服务集成

核心坐标：`com.alibaba.smart.framework:smart-engine-core:4.0.0-boot4`。

本次已验证的无数据库组合以以下依赖为入口（传递引入核心及 storage-common）：

```xml
<dependency>
  <groupId>com.alibaba.smart.framework</groupId>
  <artifactId>smart-engine-extension-storage-custom</artifactId>
  <version>4.0.0-boot4</version>
</dependency>
```

完整可复用的宿主依赖见 `standalone-consumer/pom.xml`，初始化及调用示例见 `standalone-consumer/src/test/java/consumer/StandaloneEngineTest.java`。该最小示例不需要启动 Spring 容器，也不连接外部数据库。它是集成验证测试项目，不是可直接部署的服务应用。

集成时需满足：

1. 当前产物使用 Java 25 字节码，新服务运行时需支持 Java 25。
2. 项目将多项依赖声明为 provided，宿主需补充所用功能的运行时库。示例显式提供 SLF4J、MVEL、Jackson 3、dom4j；数据库扩展还需 Spring、MyBatis、数据源及数据库驱动等。不能只复制单个核心 JAR。
3. storage-custom 的示例使用 PersisterSession，每次使用需管理会话生命周期；示例没有验证重启恢复或业务数据持久化。
4. 需要数据库持久化时，使用 storage-mysql 扩展并配置数据源、事务、Mapper 和匹配的表结构，再对目标数据库执行集成测试。本仓库 POM 使用 Spring Framework 7.0.9 和 Spring Boot 4.1.1 BOM，不能直接假定兼容旧版服务依赖组合。
5. 其他机器/CI 不能读取这台机器的本地 Maven 仓库。正式共享时需将父 POM 和所需模块发布到团队制品仓库，使用明确区分本地修复的版本；本次未执行 deploy 或远程发布。

构建中存在非阻断警告（Lombok equals/hashCode、过时/unchecked API、shade 的 MANIFEST 重叠、Maven 在 JDK 25 上的 Unsafe 提示），未通过跳过测试规避失败。
