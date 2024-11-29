## 事件管理应用

本项目是一个基于 React 的事件管理应用，主要功能包括获取事件列表、删除事件以及添加事件（通过 IncidentForm 组件）。应用通过与后端服务器（http://localhost:8080/incident）进行数据交互来实现这些功能。

### 功能

- 查看事件列表
- 添加新事件
- 删除现有事件
- 修改现有事件

### 使用的技术

- React：用于构建用户界面。
- CSS：通过 ./App.css 文件进行样式设置。
- 后端数据交互：使用 fetch API 与后端服务器进行数据的获取和删除操作。

### 先决条件

- Node.js
- npm（Node 包管理器）

### 安装
- 安装依赖:  
  ```
  npm install
  ```
- 启动应用:  
  ```
  npm start
  ```
- 打开浏览器，访问 http://localhost:3000 查看应用。
### 简单界面
![简单界面](https://github.com/user-attachments/assets/9b1ffed7-a40d-4159-8405-69881fcb4c64)

### 注意事项

- 后端服务器地址为 http://localhost:8080/incident，如果后端服务器地址或接口发生变化，需要相应地修改 fetch 请求的 URL。
- 确保网络连接正常，否则在数据获取和删除事件操作时可能会出现网络错误提示。
