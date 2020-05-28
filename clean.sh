#!/bin/bash

export javapkcs11cfg=./nitrokey-slot0.cfg

./wildfly/bin/jboss-cli.sh --connect :shutdown

rm ejbca/p12/*
rm ejbca
rm -rf .ejbca

rm wildfly
rm -rf wildfly-18.0.1.Final

rm -rf ejbca-custom


if [ -z ${CA_TOKENPASSWORD}]; then
  echo "Enter Management CA token PIN (normal User PIN): "
  read -s TEMP
  export CA_TOKENPASSWORD=$TEMP
fi

keytool -delete -keystore NONE -storetype PKCS11 -storepass ${CA_TOKENPASSWORD} -providerclass sun.security.pkcs11.SunPKCS11 -providerarg ${javapkcs11cfg} -alias empty
keytool -delete -keystore NONE -storetype PKCS11 -storepass ${CA_TOKENPASSWORD} -providerclass sun.security.pkcs11.SunPKCS11 -providerarg ${javapkcs11cfg} -alias test
keytool -delete -keystore NONE -storetype PKCS11 -storepass ${CA_TOKENPASSWORD} -providerclass sun.security.pkcs11.SunPKCS11 -providerarg ${javapkcs11cfg} -alias default
keytool -delete -keystore NONE -storetype PKCS11 -storepass ${CA_TOKENPASSWORD} -providerclass sun.security.pkcs11.SunPKCS11 -providerarg ${javapkcs11cfg} -alias signing
