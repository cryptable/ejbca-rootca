#!/bin/bash

# set -x

export javapkcs11cfg=nitrokey-slot0.cfg

kill -9 $(ps -eo pid,comm | awk '$2=="java" {print $1}')

rm ejbca
rm ejbca_ce_6_15_2_5/p12/*
rm ejbca_ce_6_15_2_5/conf/*.properties
rm -rf ejbca-custom
rm wildfly
rm -rf wildfly-10.1.0.Final

if [ -z ${CA_TOKENPASSWORD}]; then
  echo "Enter Management CA token PIN (normal User PIN): "
  read -s TEMP
  export CA_TOKENPASSWORD=$TEMP
fi

keytool -delete -keystore NONE -storetype PKCS11 -storepass ${CA_TOKENPASSWORD} -providerclass sun.security.pkcs11.SunPKCS11 -providerarg /etc/java-8-openjdk/security/${javapkcs11cfg} -alias empty
keytool -delete -keystore NONE -storetype PKCS11 -storepass ${CA_TOKENPASSWORD} -providerclass sun.security.pkcs11.SunPKCS11 -providerarg /etc/java-8-openjdk/security/${javapkcs11cfg} -alias test
keytool -delete -keystore NONE -storetype PKCS11 -storepass ${CA_TOKENPASSWORD} -providerclass sun.security.pkcs11.SunPKCS11 -providerarg /etc/java-8-openjdk/security/${javapkcs11cfg} -alias default
keytool -delete -keystore NONE -storetype PKCS11 -storepass ${CA_TOKENPASSWORD} -providerclass sun.security.pkcs11.SunPKCS11 -providerarg /etc/java-8-openjdk/security/${javapkcs11cfg} -alias signing
