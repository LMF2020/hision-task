# 简介

本项目是一个maven工程, 可以通过eclipse/ideaJ等开发工具导入进行开发调试

# 配置

数据库连接地址以及websocket消息推送的端口等默认配置都在application.properties里
打包后需要手工修改配置，在config/app.properties里修改，这里的配置会覆盖默认配置

# 运行

MainLauncher是程序入口,启动即可，浏览器访问: http://localhost:8022
 
# 环境

* 必须JDK8
* eclipse或idea等IDE开发工具,可选

# 打包

mvn clean package

