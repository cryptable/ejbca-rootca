Installation
============

Installation using EJBCA as a RootCA with Nitrokey to store the keys. This is all contained in a Raspberry Pi with an extra nice frontend for the key ceremony.
This is based on the script delivered with the EJBCA.


Testing and installation is done in 3 ways:
- Classic Ubuntu Server to install Wildfly 18.01 + EJBCA + H2DB
- QEMU Raspberry Pi emulator with Ubuntu Server
- Real Raspberry Pi model 3

Classic Ubuntu Server
---------------------

Prerequisites:
- VirtualBox
- Ubuntu 18.04 LTS
- Java openJDK latest
- Wildfly Latest
- EJBCA 6.15.2.5
- Web Frontend in Angular
- Web Backend in Java, Go or Rust: We need an application server to run EJBCA, so probably we build the application in Spring, because of all the possibilities.


###Install Ubuntu 18.04 LTS

1) Create a VirtualBox Linux image with 2Gb mem, 10Gb Disk
2) Install Ubuntu Server 18.04 LTS (username: ejbca / password: system) + openssh
3) Install VirtualBox Guest Additions for shared folders
```
sudo -i
cd
apt install gcc make perl
mount /dev/cdrom /mnt
mkdir VirtualBoxGuestAdd
cp -r /mnt/* ./VirtualBoxGuestAdd
cp ./VirtualBoxGuestAdd
./VBoxLinuxAdditions.run
cd ..
rm -rf VirtualBoxGuestAdd
reboot
```
4) Install JDK: sudo apt install default-jdk-headless
5) Mount a Shared Folder 'Downloads' from host into VirtualBox:
```
sudo mount -t vboxsf -o uid=$UID,gid=$(id -g) Downloads ~/downloads
```

###Install Wildfly
1) Download Widlfly into downloads
2) Unpack Wildfly
```
cp downloads/wildfly-18.0.1.Final.tar.gz .
tar xvzf wildfly-18.0.1.Final.tar.gz
ln -s wildfly-18.0.1.Final wildfly
```
3) patch the wildfly server (optionalyh Pi 4 at least)
```
pushd wildfly/bin
sed -i.bak 's/JAVA_OPTS="-Xms64m -Xmx512m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m -Djava.net.preferIPv4Stack=true"/JAVA_OPTS="-Xms256m -Xmx512m -XX:MetaspaceSize=128M -XX:MaxMetaspaceSize=384m -Djava.net.preferIPv4Stack=true"/g' standalone.conf
popd
```
4) start the wildfly server
```
nohup wildfly/bin/standalone.sh -b 0.0.0.0 > /dev/null 2> /dev/null &
```
In the wildlfy/standalone/log/server.log you can see the result or:
```
wildfly/bin/jboss-cli.sh --connect ":read-attribute(name=server-state)" | grep "result" | awk '{ print $3; }'|grep running
```
5) Configure wildfly server
- Add AJP listener (optionally)
```
wildfly/bin/jboss-cli.sh --connect "/subsystem=undertow/server=default-server/ajp-listener=ajp-listener:add(socket-binding=ajp, scheme=https, enabled=true)"
```
- Add H2DB datasource (http://www.h2database.com/html/features.html#products_work_with) (TODO: Encryption and securization)
```
wildfly/bin/jboss-cli.sh --connect "/subsystem=datasources/jdbc-driver=h2:add(driver-name=h2,driver-module-name=com.h2database.h2,driver-xa-datasource-class-name=org.h2.jdbcx.JdbcDataSource)"
wildfly/bin/jboss-cli.sh --connect "data-source add --name=ejbcads --driver-name=\"h2\" --jndi-name=\"java:/EjbcaDS\" --connection-url=\"jdbc:h2:file:~/.ejbcaDB;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;\" --min-pool-size=5 --max-pool-size=150 --pool-prefill=true --prepared-statements-cache-size=50 --share-prepared-statements=true --transaction-isolation=TRANSACTION_READ_COMMITTED --use-ccm=true --user-name=\"sa\" --password=\"sa\" "
wildfly/bin/jboss-cli.sh --connect command=:reload
```
- Remove ExampleDS
```
wildfly/bin/jboss-cli.sh --connect "data-source remove --name=ExmapleDS"
wildfly/bin/jboss-cli.sh --connect command=:reload
```
- 

General Notes:
- Help on Wildfly commands:
```
wildfly/bin/jboss-cli.sh --connect
> datasource add --help
```
- Check Wildfly state:
```
wildfly/bin/jboss-cli.sh --connect ":read-attribute(name=server-state)"
wildfly/bin/jboss-cli.sh --connect "read-attribute --name=server-state"
```
- Reload Wildfly:
```
wildfly/bin/jboss-cli.sh --connect command=:reload
```
- Shutdown Wildfly state:
```
wildfly/bin/jboss-cli.sh --connect command=:shutdown
```
- Configure WildFly remoting
```
wildfly/bin/jboss-cli.sh --connect "/subsystem=remoting/http-connector=http-remoting-connector:remove"
wildfly/bin/jboss-cli.sh --connect "/subsystem=remoting/http-connector=http-remoting-connector:add(connector-ref=\"remoting\",security-realm=\"ApplicationRealm\")"
wildfly/bin/jboss-cli.sh --connect "/socket-binding-group=standard-sockets/socket-binding=remoting:add(port=\"4447\")"
wildfly/bin/jboss-cli.sh --connect "/subsystem=undertow/server=default-server/http-listener=remoting:add(socket-binding=remoting)"
wildfly/bin/jboss-cli.sh --connect command=:reload
```
- Configure WildFly logging
```
wildfly/bin/jboss-cli.sh --connect "/subsystem=logging/logger=org.ejbca:add"
wildfly/bin/jboss-cli.sh --connect "/subsystem=logging/logger=org.ejbca:write-attribute(name=level, value=DEBUG)"
wildfly/bin/jboss-cli.sh --connect "/subsystem=logging/logger=org.cesecore:add"
wildfly/bin/jboss-cli.sh --connect "/subsystem=logging/logger=org.cesecore:write-attribute(name=level, value=DEBUG)"
```
- Configure WildFly remove HTTPS and HTTP connections
```
wildfly/bin/jboss-cli.sh --connect "/subsystem=undertow/server=default-server/http-listener=default:remove"
wildfly/bin/jboss-cli.sh --connect "/subsystem=undertow/server=default-server/https-listener=https:remove"
wildfly/bin/jboss-cli.sh --connect "/socket-binding-group=standard-sockets/socket-binding=http:remove"
wildfly/bin/jboss-cli.sh --connect "/socket-binding-group=standard-sockets/socket-binding=https:remove"
wildfly/bin/jboss-cli.sh --connect command=:reload
```

###Install EJBCA
1) Download EJBCA into downloads
2) install unzip and ant
```
sudo apt install unzip ant
```
3) unpack EJBCA
```
cp downloads/ejbca_ce_6_15_2_5.zip .
unzip ejbca_ce_6_15_2_5.zip
ln -s ./ejbca_ce_6_15_2_5 ejbca
```
4) Initialize the configuration files in write in ejbca_custom
```
mkdir -p ejbca-custom/conf
cp ejbca/conf/batch.properties.sample ejbca-custom/conf/batch.properties
cp ejbca/conf/certstore.properties.sample ejbca-custom/conf/certstore.properties
cp ejbca/conf/cesecore.properties.sample ejbca-custom/conf/cesecore.properties
cp ejbca/conf/crlstore.properties.sample ejbca-custom/conf/crlstore.properties
cp ejbca/conf/database.properties.sample ejbca-custom/conf/database.properties
cp ejbca/conf/ejbca.properties.sample ejbca-custom/conf/ejbca.properties
cp ejbca/conf/install.properties.sample ejbca-custom/conf/install.properties
cp ejbca/conf/web.properties.sample ejbca-custom/conf/web.properties
```
5) Build and deply ejbca
When using Java 11, you need to rebuild ServiceManifestBuilder:
```
sudo apt install ant ant-optional svn
svn co https://svn.cesecore.eu/svn/ejbca/trunk/buildtools
pushd buildtools/servicemanifestbuilder
ant
popd
cp buildtools/servicemanifestbuilder/dist/servicemanifestbuilder-1.0.1.jar ejbca/lib/ext
rm ejbca/lib/ext/servicemanifestbuilder-1.0.0.jar 
```
Download the com.sun.xml jaxws-ri from a repository and unzip it in the ejbca/lib directory

Modify the build-properties.xml under modules directory, add in the path declaration:
```
        <path id="lib.jaxws-ri.classpath"><fileset dir="${ejbca.home}/lib/jaxws-ri/lib" includes="*.jar"/></path>
```
Modify the build.xml under modules/ejbca-ws-cli, add in the compile.class path (Java 11 doesn't include JaxB anymore):
```
        <path refid="lib.jaxws-ri.classpath"/>
```
Modify the docs.xmli for the JavaDoc, add in the jdoc.classpath path (Java 11 doesn't include JaxB anymore):
```
        <fileset dir="${ejbca.home}/lib/jaxws-ri/lib" includes="*.jar"/>
```

Build ejbca
```
pushd ejbca
ant clean deployear
```
You can add to 2 Deprecated annotation to prevent the warnings

Initialize the ejbca
```
ant clean deployear
sleep 120
ant deploy-keystore
popd
```

###Setup Wildfly https connectors

```
wildfly/bin/jboss-cli.sh --connect "/interface=http:add(inet-address=\"0.0.0.0\")"
wildfly/bin/jboss-cli.sh --connect "/interface=httpspub:add(inet-address=\"0.0.0.0\")"
wildfly/bin/jboss-cli.sh --connect "/interface=httpspriv:add(inet-address=\"0.0.0.0\")"
wildfly/bin/jboss-cli.sh --connect "/socket-binding-group=standard-sockets/socket-binding=http:add(port="8080",interface=\"http\")"
wildfly/bin/jboss-cli.sh --connect "/subsystem=undertow/server=default-server/http-listener=http:add(socket-binding=http)"
wildfly/bin/jboss-cli.sh --connect ":reload"
```

You need some configuration passwords found in the ejbca-custom/web.properties
```
grep '^httpsserver.password' ejbca-custom/conf/web.properties | awk -F= '{ print $2 }' | grep -v '^$'
grep '^java.trustpassword' ejbca-custom/conf/web.properties | awk -F= '{ print $2 }' | grep -v '^$'
grep '^httpsserver.hostname' ejbca-custom/conf/web.properties | awk -F= '{ print $2 }' | grep -v '^$'
```

gives you values for
keystore_password -> 9a6d66095ec407de760260c5d8ba26e073b43db5
web_hostname -> localhost
truststore_password -> e959fb068072263455df13b5230b3821e17e4a91


Start the SSL configuration
```
wildfly/bin/jboss-cli.sh --connect "/core-service=management/security-realm=SSLRealm:add()"
wildfly/bin/jboss-cli.sh --connect "/core-service=management/security-realm=SSLRealm/server-identity=ssl:add(keystore-relative-to=\"jboss.server.config.dir\", keystore-path=\"keystore/keystore.jks\", keystore-password=\"${keystore_password}\", alias=\"${web_hostname}\")"
wildfly/bin/jboss-cli.sh --connect "/core-service=management/security-realm=SSLRealm/authentication=truststore:add(keystore-relative-to=\"jboss.server.config.dir\", keystore-path=\"keystore/truststore.jks\", keystore-password=\"${truststore_pass}\")"
wildfly/bin/jboss-cli.sh --connect "/socket-binding-group=standard-sockets/socket-binding=httpspriv:add(port="8443",interface=\"httpspriv\")"
wildfly/bin/jboss-cli.sh --connect "/socket-binding-group=standard-sockets/socket-binding=httpspub:add(port="8442", interface=\"httpspub\")"
wildfly/bin/jboss-cli.sh --connect "/subsystem=undertow/server=default-server/http-listener=http:write-attribute(name=redirect-socket, value=\"httpspriv\")"
wildfly/bin/jboss-cli.sh --connect ":shutdown"
nohup wildfly/bin/standalone.sh -b 0.0.0.0 > /dev/null 2> /dev/null &
```

Wait until running

```
wildfly/bin/jboss-cli.sh --connect "/subsystem=undertow/server=default-server/https-listener=httpspriv:add(socket-binding=httpspriv, security-realm=\"SSLRealm\", verify-client=REQUIRED)"
wildfly/bin/jboss-cli.sh --connect "/subsystem=undertow/server=default-server/https-listener=httpspub:add(socket-binding=httpspub, security-realm=\"SSLRealm\")"
wildfly/bin/jboss-cli.sh --connect ":reload"
```

Wait until running

```
wildfly/bin/jboss-cli.sh --connect "/system-property=org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH:add(value=true)"
wildfly/bin/jboss-cli.sh --connect "/system-property=org.apache.catalina.connector.CoyoteAdapter.ALLOW_BACKSLASH:add(value=true)"
wildfly/bin/jboss-cli.sh --connect "/system-property=org.apache.catalina.connector.URI_ENCODING:add(value=\"UTF-8\")"
wildfly/bin/jboss-cli.sh --connect "/system-property=org.apache.catalina.connector.USE_BODY_ENCODING_FOR_QUERY_STRING:add(value=true)"
wildfly/bin/jboss-cli.sh --connect "/subsystem=webservices:write-attribute(name=wsdl-host, value=jbossws.undefined.host)"
wildfly/bin/jboss-cli.sh --connect "/subsystem=webservices:write-attribute(name=modify-wsdl-address, value=true)"
wildfly/bin/jboss-cli.sh --connect ":reload"
```

Wait until running


### Setup the logging
```
wildfly/bin/jboss-cli.sh --connect "/subsystem=logging/logger=org.ejbca:write-attribute(name=level, value=DEBUG)"
wildfly/bin/jboss-cli.sh --connect "/subsystem=logging/logger=org.cesecore:write-attribute(name=level, value=DEBUG)"
wildfly/bin/jboss-cli.sh --connect ":reload"
```

Wait until running

### Update the ejbca-setup.dh
We need to update the ejbca-setup.sh script to support the Wildfly 18.


###Setup the NitroKey

To support USB in Linux Host under Virtualbox guest:
```
sudo adduser $USER vboxusers
```
Assign the NitroKey to the VirtualBox (USB)

Install the Nitrokey driver
```
sudo apt install opensc
```

Add the NitroKey support to java by creating a nitro configuration file (nitro-slot0.cfg). This allow you to use Nitrokey with keytool.
(https://docs.oracle.com/javase/7/docs/technotes/guides/security/p11guide.html)
```
name = NitrokeyHSM
library = /usr/lib/x86_64-linux-gnu/opensc-pkcs11.so
slot = 0

attributes(*, CKO_PUBLIC_KEY, *) = {
  CKA_TOKEN = false
  CKA_ENCRYPT = true
  CKA_VERIFY = true
  CKA_WRAP = true
}
attributes(*, CKO_PRIVATE_KEY, *) = {
  CKA_DERIVE = false
  CKA_TOKEN = true
  CKA_PRIVATE = true
  CKA_SENSITIVE = true
  CKA_EXTRACTABLE = false
  CKA_DECRYPT = true
  CKA_SIGN = true
  CKA_UNWRAP = true
}
attributes(*, CKO_SECRET_KEY, *) = {
  CKA_SENSITIVE = true
  CKA_EXTRACTABLE = false
  CKA_ENCRYPT = true
  CKA_DECRYPT = true
  CKA_SIGN = true
  CKA_VERIFY = true
  CKA_WRAP = true
  CKA_UNWRAP = true
}
```

Useful commands:
list key:
```
keytool -v -list -keystore NONE -storetype PKCS11 -storepass 123456 -providerclass sun.security.pkcs11.SunPKCS11 -providerarg ./nitrokey-slot0.cfg
```
delete key:
```
keytool -v -delete -keystore NONE -storetype PKCS11 -storepass 123456 -providerclass sun.security.pkcs11.SunPKCS11 -providerarg ./nitrokey-slot0.cfg -alias rootCADefault
```
debugging pkcs11:
(https://github.com/OpenSC/OpenSC/wiki/Using-OpenSC)


###Web Frontend in Angular
- xterm.js to access into a shell on the raspberry Pi for real problems.

###Web Backend in Java, Go or Rust
