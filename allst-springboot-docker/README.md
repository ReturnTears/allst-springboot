# Docker技术入门与实战(第一版)
```text
《Docker技术入门与实战》

前言

◆ 一台服务器上同时运行一千个Docker容器，这已经成为现实


第一部分 Docker入门

◆ 云计算时代，虚拟化技术无疑是整座信息技术大厦最核心的一块基石。

◆ 虚拟化既可以通过硬件模拟来实现，也可以通过操作系统来实现。

◆ Docker是基于Go语言实现的云开源项目

◆ Docker是仅次于OpenStack的最受欢迎的云计算开源项目。

◆ 现在主流的Linux操作系统都已经支持Docker。例如，Redhat RHEL 6.5/ CentOS 6.5往上的操作系统、Ubuntu 14.04操作系统，都已经默认带有Docker软件包。

◆ Docker的主要目标是“Build, Ship and Run Any App, Anywhere”，即通过对应用组件的封装（Packaging）、分发（Distribution）、部署（Deployment）、运行（Runtime）等生命周期的管理，达到应用组件级别的“一次封装，到处运行”。

◆ Docker基于Linux的多项开源技术提供了高效、敏捷和轻量级的容器方案，并且支持在多种主流云平台（PaaS）和本地系统上部署。可以说Docker为应用的开发和部署提供了“一站式”的解决方案。

◆ Docker引擎的基础是Linux容器（Linux Containers，LXC）技术

◆ 容器有效地将由单个操作系统管理的资源划分到孤立的组中，以便更好地在孤立的组之间平衡有冲突的资源使用需求。

◆ Docker提供了各种容器管理工具（如分发、版本、移植等）让用户无需关注底层的操作，可以简单明了地管理和使用容器。

◆ 每个容器内运行一个应用，不同的容器相互隔离，容器之间也可以建立通信机制。

◆ Docker容器虚拟化的好处Docker项目的发起人和Docker Inc.的CTO Solomon Hykes认为，Docker在正确的地点、正确的时间顺应了正确的趋势—即高效地构建应用。现在开发者需要能方便地创建运行在云平台上的应用，也就是说应用必须能够脱离底层机器，而且同时必须是“任何时间任何地点”可获取的。因此，开发者们需要一种创建分布式应用程序的方式，这也是Docker所能够提供的。

◆ Docker在开发和运维过程中，具有如下几个方面的优势。

◆ 更快速的交付和部署。使用Docker，开发人员可以使用镜像来快速构建一套标准的开发环境；开发完成之后，测试和运维人员可以直接使用相同环境来部署代码。Docker可以快速创建和删除容器，实现快速迭代，大量节约开发、测试、部署的时间。并且，各个步骤都有明确的配置和操作，整个过程全程可见，使团队更容易理解应用的创建和工作过程

◆  更高效的资源利用。Docker容器的运行不需要额外的虚拟化管理程序（Ⅴirtual Machine Manager，ⅤMM，以及Hypervisor）支持，它是内核级的虚拟化，可以实现更高的性能，同时对资源的额外需求很低

◆  更轻松的迁移和扩展。Docker容器几乎可以在任意的平台上运行，包括物理机、虚拟机、公有云、私有云、个人电脑、服务器等。 这种兼容性让用户可以在不同平台之间轻松地迁移应用

◆ 更简单的更新管理。使用Dockerfile，只需要小小的配置修改，就可以替代以往大量的更新工作。并且所有修改都以增量的方式进行分发和更新，从而实现自动化并且高效的容器管理

◆ 作为一种轻量级的虚拟化方式，Docker在运行应用上跟传统的虚拟机方式相比具有显著优势：

◆ Docker容器很快，启动和停止可以在秒级实现，这相比传统的虚拟机方式要快得多。

◆ Docker容器对系统资源需求很少，一台主机上可以同时运行数千个Docker容器。

◆  Docker通过类似Git的操作来方便用户获取、分发和更新应用镜像，指令简明，学习成本较低。

◆ Docker通过Dockerfile配置文件来支持灵活的自动化创建和部署机制，提高工作效率。

◆ Docker容器除了运行其中的应用之外，基本不消耗额外的系统资源，保证应用性能的同时，尽量减小系统开销。传统虚拟机方式运行N个不同的应用就要启动N个虚拟机（每个虚拟机需要单独分配独占的内存、磁盘等资源），而Docker只需要启动N个隔离的容器，并将应用放到容器内即可

◆ 在隔离性方面，传统的虚拟机方式多了一层额外的隔离。但这并不意味着Docker就不安全。Docker利用Linux系统上的多种防护机制实现了严格可靠的隔离

◆ 从1.3版本开始，Docker引入了安全选项和镜像签名机制，极大地提高了使用Docker的安全性。

◆ 虚拟化技术是一个通用的概念，在不同领域有不同的理解。在计算领域，一般指的是计算虚拟化（Computing Ⅴirtualization），或通常说的服务器虚拟化

◆ 在计算机技术中，虚拟化（Ⅴirtualization）是一种资源管理技术，是将计算机的各种实体资源，如服务器、网络、内存及存储等，予以抽象、转换后呈现出来，打破实体结构间的不可切割的障碍，使用户可以用比原本的组态更好的方式来应用这些资源

◆ 虚拟化的核心是对资源进行抽象，目标往往是为了在同一个主机上运行多个系统或应用，从而提高系统资源的利用率，同时带来降低成本、方便管理和容错容灾等好处。

◆ 从大类上分，虚拟化技术可分为基于硬件的虚拟化和基于软件的虚拟化

◆ 真正意义上的基于硬件的虚拟化技术不多见，少数如网卡中的单根多IO虚拟化（Single Root I/OⅤirtualization and Sharing Specification，SR-IOⅤ）等技术

◆ 基于软件的虚拟化从对象所在的层次，又可以分为应用虚拟化和平台虚拟化（通常说的虚拟机技术即属于这个范畴）。其中，前者一般指的是一些模拟设备或Wine这样的软件

◆ 后者又可以细分为如下几个子类：

◆ 完全虚拟化。虚拟机模拟完整的底层硬件环境和特权指令的执行过程，客户操作系统无需进行修改。例如ⅤMware Workstation、ⅤirtualBox、QEMU等

◆ 硬件辅助虚拟化。利用硬件（主要是CPU）辅助支持（目前x86体系结构上可用的硬件辅助虚拟化技术包括Intel-ⅤT和AMD-Ⅴ）处理敏感指令来实现完全虚拟化的功能，客户操作系统无需修改，例如ⅤMware Workstation、Xen、KⅤM

◆ 部分虚拟化。只针对部分硬件资源进行虚拟化，客户操作系统需要进行修改。现在有些虚拟化技术的早期版本仅支持部分虚拟化

◆ 超虚拟化（Paravirtualization）。部分硬件接口以软件的形式提供给客户机操作系统，客户操作系统需要进行修改，例如早期的Xen

◆ 操作系统级虚拟化。内核通过创建多个虚拟的操作系统实例（内核和库）来隔离不同的进程。容器相关技术即在这个范畴

◆ Docker以及其他容器技术都属于操作系统的虚拟化这个范畴

◆ Docker虚拟化方式之所以拥有众多优势，这跟操作系统的虚拟化自身的特点是分不开的。

◆ 传统方式是在硬件层面实现虚拟化，需要有额外的虚拟机管理应用和虚拟机操作系统层。

◆ Docker容器是在操作系统层面上实现虚拟化，直接复用本地主机的操作系统，因此更加轻量级。

```

# 将项目打包为Docker镜像
```text
构建镜像
docker build -t <image_name>:<tag> .
示例：
docker build -t allst-springboot-docker:v1 .

运行镜像
docker run -p host-port:container-port --name container-name your-image-name:tag
示例
docker run -p 8099:8099 --name allst-springboot-docker allst-springboot-docker:v1
docker run -d -p 8099:8099 --name allst-springboot-docker-1.0-SNAPSHOT allst-springboot-docker:v1

删除镜像
docker rmi <image_name>:<tag>


删除容器
docker rm -f <container_name>

关闭正在运行的容器
docker stop <container_name>
docker stop 2c531267fa0d
```