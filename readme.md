# IDEA日志分析工具

@created by yuanbao at 202503



### 本工具作用：

在上个自研工具CollectIdeaLog.exe实现自动抓取全部的IDEA日志并上传至FTP后，本工具用于对IDEA日志进行分析处理。



### 实现：

1. 按人员多日志分析处理；
2. 支持按日和按月模式；
3. 支持导出为txt和excel；
4. 增加导出jar，并可传参日期、路径调用；
5. 在分析通义灵码插件基础上，增加Github Copilot的分析；
6. 增加jar转可执行exe程序；
7. 增加可视化交互操作界面；



### 来源数据：采集到的日志列表结构：

<img src="https://yuanbao-oss.oss-cn-shenzhen.aliyuncs.com/img/public_imgs/PicGo/202503041710506.png" alt="image-20250304171018140" style="zoom:67%;" />



## jar包使用的命令行

```shell
java -jar ./LogAnalysis-1.0-SNAPSHOT.jar 20250304 C:\Users\yuanbao\Desktop\IDEA日志收集\2025-03-04

```

java -jar ./LogAnalysis-1.0-SNAPSHOT.jar 后可接三个参数，分别是：

- 输入日期：
  - 20250304  即为日模式，统计此日期及前一周内，按日统计
  - 202503 即为月模式，统计此月份及前半年内，按月份统计
  - 可以为空，默认表示为当日
- 日志路径：
  - 为日志文件夹的路径
  - 可以为空，默认表示jar包所在目录
- 结果路径
  - 可以为空，默认表示与日志路径相同





### 分析结果样式：

![image-20250304171153760](https://yuanbao-oss.oss-cn-shenzhen.aliyuncs.com/img/public_imgs/PicGo/202503041711009.png)



### 用户交互操作界面：

<img src="https://yuanbao-oss.oss-cn-shenzhen.aliyuncs.com/img/public_imgs/PicGo/202503041722201.png" alt="image-20250304172200438" style="zoom:80%;" />

