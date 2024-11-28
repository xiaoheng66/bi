# 🌟 智能AI数据分析平台 | Intelligent Data Analysis Platform

## 🛠️ 技术栈 | Tech Stack
- **后端技术 | Backend Technologies**:  
  SpringBoot, MyBatis, RabbitMQ, AIGC, Redisson, Easy Excel, 数据万象 (Tencent Cloud CI)

---

## 🚀 项目介绍 | Project Overview

### 📋 中文 | In Chinese
本项目是基于 **SpringBoot**、**MyBatis**、**RabbitMQ**、**AIGC**、**Redisson**、**Easy Excel** 和 **数据万象（Tencent Cloud CI）** 打造的智能数据分析平台。

核心功能和特点包括：
1. 用户仅需输入分析诉求并上传excel文件数据, 系统利用Easy Excel自动转化为csv格式数据处理
2. 平台通过 AIGC 接口(使用讯飞星火提供的api)自动生成分析结论和可视化图表；
3. **RabbitMQ** 用于实现异步处理，优化性能；
4. **数据万象** 用于敏感词审核，确保内容合规；
5. **Redisson** 提供限流支持，保障系统在高并发场景下的稳定性；

适用场景：智能商业分析、数据报告自动化生成、敏感内容审核等。

---

### 📋 English | In English
This project is an **Intelligent Data Analysis Platform** built with **SpringBoot**, **MyBatis**, **RabbitMQ**, **AIGC**, **Redisson**, **Easy Excel**, and **Tencent Cloud CI**.

Key features include:
1. Users only need to input their analysis requirements and upload an Excel file; the system automatically converts it into CSV format using Easy Excel for further processing.
2. The platform leverages AIGC APIs to automatically generate analysis conclusions and visualization charts;
3. **RabbitMQ** enables asynchronous processing to optimize performance;
4. **Tencent Cloud CI** (Data Vison) is used for sensitive word auditing to ensure compliance;
5. **Redisson** provides rate-limiting to maintain stability under high concurrency;

Applications: Intelligent business analysis, automated data reporting, sensitive content auditing, and more.

---

## ✨ 核心亮点 | Key Features
- **📊 智能分析 | AI-Driven Analysis**  
  自动生成分析报告和可视化图表。  
  Automatically generates analysis reports and visualization charts.

- **🚀 性能优化 | Performance Optimization**  
  基于 RabbitMQ 实现异步处理，提升响应速度和系统吞吐量。  
  Asynchronous processing powered by RabbitMQ to enhance response time and system throughput.

- **🔍 内容合规 | Content Compliance**  
  通过数据万象实现敏感词审核，确保上传内容合法合规。  
  Sensitive word auditing via Tencent Cloud CI to ensure uploaded content is compliant.

- **💡 限流保障 | Rate-Limiting Support**  
  使用 Redisson 实现限流，保障高并发环境下的服务稳定性。  
  Rate-limiting powered by Redisson ensures service stability in high-concurrency scenarios.

- **📈 高效易用 | Efficiency & Usability**  
  用户友好的设计，一键操作获取分析结果。  
  User-friendly design for one-click operation to generate analysis results.
