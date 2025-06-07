<<<<<<< HEAD
# Jodo - Android 待办事项应用

## 项目简介

Jodo 是一个功能丰富的Android待办事项应用，专注于提供简洁高效的任务管理体验。

## 主要功能

### 任务管理
- ✅ 添加、编辑、删除任务
- ✅ 标记任务完成状态
- ✅ 收藏重要任务
- ✅ 设置任务截止日期
- ✅ 任务提醒功能
- ✅ 重复任务设置

### 分类管理
- ✅ 创建自定义分类
- ✅ 拖拽排序分类
- ✅ 分类颜色标识
- ✅ 按分类筛选任务

### 筛选功能
- ✅ 全部任务
- ✅ 进行中任务
- ✅ 已完成任务
- ✅ 收藏任务
- ✅ 今日任务
- ✅ 按分类筛选

### 统计功能
- ✅ 任务完成统计
- ✅ 分类任务数量统计
- ✅ 可视化图表展示

### 用户界面
- ✅ Material Design 设计规范
- ✅ 深色/浅色主题
- ✅ 直观的侧边栏导航
- ✅ 流畅的动画效果
- ✅ 响应式布局

## 技术特点

### 架构
- **MVVM架构模式** - 使用ViewModel和LiveData
- **Repository模式** - 数据访问层抽象
- **单一数据源** - StateFlow统一状态管理

### 技术栈
- **Kotlin** - 100% Kotlin开发
- **Android Jetpack** - ViewModel, LiveData, Navigation等
- **Material Design Components** - 现代化UI组件
- **Coroutines** - 异步编程
- **Gson** - JSON序列化
- **WorkManager** - 后台任务调度

### 数据存储
- **本地JSON存储** - 轻量级数据持久化
- **类型安全** - 强类型数据模型
- **数据迁移** - 版本兼容性支持

## 项目结构

```
app/src/main/java/takagicom/todo/jodo/
├── adapter/           # RecyclerView适配器
├── model/            # 数据模型
├── repository/       # 数据仓库
├── service/          # 后台服务
├── ui/              # UI界面
├── utils/           # 工具类
├── viewmodel/       # ViewModel层
└── MainActivity.kt  # 主活动
```

## 安装要求

- **最低Android版本**: Android 7.0 (API 24)
- **目标Android版本**: Android 14 (API 34)
- **Gradle版本**: 8.7
- **Kotlin版本**: 1.9.22

## 编译构建

1. 克隆项目
```bash
git clone https://github.com/yourusername/jodo.git
cd jodo
```

2. 打开Android Studio导入项目

3. 构建项目
```bash
./gradlew assembleDebug
```

4. 安装到设备
```bash
./gradlew installDebug
```

## 功能截图

*待补充应用截图*

## 版本历史

### v1.0.0 (当前版本)
- 基础任务管理功能
- 分类系统
- 筛选功能
- 提醒功能
- 重复任务
- 统计功能

## 贡献指南

欢迎提交Issue和Pull Request来改进这个项目！

## 许可证

本项目采用GPL许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 联系方式

如有问题或建议，请通过以下方式联系：
- 提交GitHub Issue
- 发送邮件到: [joyin8888@foxmail.com]

-
=======
# Jodo-Android
>>>>>>> 4d8dea0dfb4993fe92ca1ad16273f3384896ddd2
