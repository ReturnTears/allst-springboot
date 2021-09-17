# MinIo

## MinIo官网

https://min.io/

## 安装与使用

https://docs.min.io/docs/minio-quickstart-guide.html

## 使用场景
互联网海量非结构化数据的存储需求
+ 电商网站：海量商品图片
+ 视频网站：海量视频文件
+ 网盘相关：海量文件
+ 社交网站：海量图片

## MinIo介绍

非常适合于存储大量非结构化的文件

Minio的优点：

- 部署简单：一个single二进制文件即是一切，还可支持各种平台
- minio支持海量存储，可按zone扩展（原zone不受任何影响），支持单格对象最大5TB
- 兼容Amazon S3接口， 充分考虑开发人员的需求和体验
- 低冗余且磁盘损坏高容忍，标准且更高的数据冗余系数为2（即存储一个1M的数据对象，实际占用磁盘空间为2M）。但在任意n/2块disk损坏的情况下依然可以读出数据（n为一个纠删码集合（Erasure Coding Set）中的disk数量）。并且这种损坏恢复是基于单个对象的，而不是基于整个存储卷的。
- 读写性能优异

纠删码EC（Erasure Code）:

Minio使用纠删码机制来保护高可靠性，使用highwayhash来处理数据损坏（）



存储形式：

文件对象上传到MinIo，会在对应的数据存储磁盘中，以Bucket名称为目录，文件名称为一下级目录，文件名下是part.1和xl.meta（老版本，最新版本如下图），前者是编码数据块及校验快，后者是元数据文件。



## MinIo基本概念

**Object**:存储到Minio的基本对象，如：文件，字节流，Anything

**Bucket**:用来存储Object的逻辑空间。每个Bucket之间的数据是相互隔离的。对于客户端而言，就相当于一个存放文件的顶层文件夹。

**Drive**:即存储数据的磁盘，在Minio启动时，以参数的方式传入。Minio中所有的对象数据都会存储在Drive里。

**Set**:即一组Drive的集合，分布式部署根据集群规模自动划分一个或多个set,每个set中的Drive分布在不同的位置。一个对象存储在一个Set上。（for example:{1...64}is divided into 4 sets each of size 16）

- 一个对象存储在一个Set上
- 一个集群划分多个Set
- 一个Set包含的Drive数量是固定的，默认由系统根据集群规模自动计算得出
- 一个Set中的Drive尽可能分布在不同的节点上



Minio环境搭建

