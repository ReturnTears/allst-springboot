# MySQL to ElasticSearch
```text
基于binlog实现数据同步的方案有两种：
一种是mysql-binlog-connector， 另一种是ali的canal。

基于第一种方式实现将MySQL数据同步到ES
查看binlog日志是否开启
show variables like 'log_bin';
OFF表示未开启ON表示开启
开启如下：
[mysqld]
#开启binlog
server_id=1
log_bin=mysql-bin
binlog_row_image=FULL
binlog-format=Row

查看日志文件及其大小
show binary logs;

show master status;

1. 配置mysql-binlog-connector

```
# SpringBoot 
```text
使用 @Autowired 注入一个 Map<String, 接口类型> 的方式通常用于以下场景和设计模式：

场景
多实现类管理:
当你需要处理多个实现同一接口的不同类时，这种方式可以让你轻松地管理这些实现类。例如，你可能有不同的事件处理器来处理相同类型的事件，但处理方式不同。
动态选择实现类:
当你需要在运行时动态地选择一个实现类来处理特定任务时，这种注入方式非常有用。例如，你可能有一个事件处理服务，根据事件的类型动态选择合适的处理器。
扩展性和灵活性:
当系统需要容易地扩展新的功能时，这种方式可以让新功能的添加变得更加简单。例如，你可能希望轻松地添加新的消息处理器，而不需要修改现有的代码。

设计模式
工厂模式:
当你需要根据不同的条件选择不同的实现类时，可以使用工厂模式来创建实例。在这种情况下，你可以创建一个工厂类来根据传入的参数选择正确的实现类。
策略模式:
策略模式允许算法在其客户端中动态地互换。使用 Map<String, 接口类型> 的注入方式，可以根据策略名称动态地选择策略。
观察者模式:
在观察者模式中，一个主题维护着它的观察者列表，并在状态发生变化时通知它们。使用 Map<String, 接口类型> 可以方便地管理多个观察者实现。

```
