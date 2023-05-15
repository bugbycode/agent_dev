# agent_dev

这是一个使用netty实现网络代理的简单实例，本项目仅供学习研究使用。此项目经多次重构修复许多bug，当前版本启用了双向认证，并且使用RSA加密代理通道，需要使用者自己签发证书。

## 自签发数字证书

1、创建数字证书颁发机构 CAManager.p12
```
keytool -genkeypair -keystore CAManager.p12 -dname "CN=证书颁发中心,C=CN" -keypass "china@" -storepass "china@" -alias CAManager -keyalg RSA -keysize 4096 -sigalg SHA256withRSA -storetype "PKCS12" -validity 3650
```
2、导出颁发机构根证书 CAManager.cer
```
keytool -exportcert -alias CAManager -keystore CAManager.p12 -file CAManager.cer -storepass "china@" -storetype "PKCS12"
```

3、创建服务端使用的秘钥库 server.p12
```
keytool -genkeypair -keystore server.p12 -dname "CN=example.com,C=CN" -keypass "china@" -storepass "china@" -alias server -keyalg RSA -keysize 4096 -sigalg SHA256withRSA -storetype "PKCS12" -validity 1000
```
4、创建服务端证书请求 server.csr
```
keytool -certreq -alias server -keystore server.p12 -file server.csr -keypass "china@" -storepass "china@"
```
5、使用证书请求文件 server.csr 到证书颁发机构 CAManager.p12 申请服务端使用的证书 server.cer
```
keytool -gencert -alias CAManager -keystore CAManager.p12 -infile server.csr -outfile server.cer -keypass "china@" -storepass "china@" -validity 1000
```
6、将数字证书颁发机构根证书CAManager.cer导入server.p12中
```
keytool -importcert -alias CAManager -file CAManager.cer -keystore server.p12 -keypass "china@" -storepass "china@" -storetype "PKCS12"
```
7、将第5步骤得到的server.cer导入到第3步骤得到的server.p12中
```
keytool -importcert -alias server -keystore server.p12 -storetype "PKCS12" -keypass "china@" -storepass "china@" -file server.cer
```
8、创建客户端使用的秘钥库 client.p12
```
keytool -genkeypair -keystore client.p12 -dname "CN=client,C=CN" -keypass "china@" -storepass "china@" -alias client -keyalg RSA -keysize 4096 -sigalg SHA256withRSA -storetype "PKCS12" -validity 1000
```
9、创建客户端证书请求 client.csr
```
keytool -certreq -alias client -keystore client.p12 -file client.csr -keypass "china@" -storepass "china@"
```
10、使用证书请求文件 client.csr 到证书颁发机构 CAManager.p12 申请服务端使用的证书 client.cer
```
keytool -gencert -alias CAManager -keystore CAManager.p12 -infile client.csr -outfile client.cer -keypass "china@" -storepass "china@" -validity 1000
```
11、将数字证书颁发机构根证书CAManager.cer导入client.p12中
```
keytool -importcert -alias CAManager -file CAManager.cer -keystore client.p12 -keypass "china@" -storepass "china@" -storetype "PKCS12"
```
12、将第10步骤得到的client.cer导入到第8步骤得到的client.p12中
```
keytool -importcert -alias client -keystore client.p12 -storetype "PKCS12" -keypass "china@" -storepass "china@" -file client.cer
```

## 代理客户端配置

1、修改network-agent配置文件src/main/resources/application.yml中的spring.netty.auth.host为network-server所部属的ip或域名如下所示：
```
server:
  port: 8787
spring:
#  main:
#    web-application-type: NONE
  keystore:
    path: client.p12
    password: china@
  server:
    datasource:
      url: jdbc:sqlite:data.db
      driverClassName: org.sqlite.JDBC
  netty: 
    agent: 
      port: 50000 #自己电脑internet代理端口
    auth: 
      host: localhost #network-server 部署的ip地址
      port: 36500 #network-server 配置的代理端口
loggin:
  config: classpath:log4j2.xml      
```
## 代理转发服务配置

1、修改network-server配置文件src/main/resources/application.yml中的spring.netty.port为自己希望监听的端口如下所示：
```
spring:
  main:
    web-application-type: NONE
  keystore:
    path: server.p12
    password: china@
  netty:
    port: 36500 #代理端口
    so_backlog: 4096
    nThreads: 4096
loggin:
  config: classpath:log4j2.xml
```
## 部署方式
在境外服务器上部署network-server 在自己电脑上部署network-agent

## 使用方式
以Windows为例可以通过以下方式设置代理服务器：

1、控制面板 -> 网络和Internet -> Internet选项 -> 在Internet属性窗口中选择连接选项

2、在局域网（LAN）设置中勾选上“为Lan使用代理服务器（这些设置不用于拨号或VPN连接）(X)” 并填写上地址为 127.0.0.1 端口 50000 

3、点击“确定”按钮完成代理配置即可。

## 站点转发管理地址

http://localhost:8787
