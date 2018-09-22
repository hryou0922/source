## java工程主要对jdk源码进行阅读，并带上笔记 (jdk的版本:jdk-e03f9868f7df)
- 说明
    - java源码阅读方法说明 参考文献[万能的林萧说：一篇文章教会你，如何做到招聘要求中的“要有扎实的Java基础”。](http://www.zuoxiaolong.com/html/article_232.html)
    - 从jdk-e03f9868f7df(1.8)中将重点学习的java复制到此工程，进行学习并标注中文
    - 一般对于异常和标志为@Deprecated类或方法不进行学习、很明显不会使用到的类（如JapaneseImperialCalendar）从本工程中删除
    
- 源码阅读
- cat: 别人使用netty实现http/websocket的服务器
- netty-all:
    - netty源码
- 工具类：学习用法
    - java.io
        - 不需要看源码的类: ObjectInputStream
    - java.lang
        - Math.java: 数学类
        - 不需要看源码的类
    - java.util
        - Collections：集合工具类
        - 以下类不看源码，只关心用法：Calendar、Date
        - 以下类直接删除：
        
    - [JDK1.6](http://download.oracle.com/technetwork/java/javase/6/docs/zh/api/java/util/Comparator.html)