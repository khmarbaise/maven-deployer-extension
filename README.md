Maven Deployer Extension
========================

[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/khmarbaise/maven-deployer-extension.svg?label=License)](http://www.apache.org/licenses/)
[![Maven Central](https://img.shields.io/maven-central/v/com.soebes.maven.extensions/maven-deployer-extension.svg?label=Maven%20Central)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.soebes.maven.extensions%22%20a%3A%22maven-deployer-extension%22)
[![Build Status](https://travis-ci.org/khmarbaise/maven-deployer-extension.svg?branch=master)](https://travis-ci.org/khmarbaise/maven-deployer-extension)

Overview
--------

 The idea of the extension is to handle  `deployAtEnd` of the
 maven-deploy-plugin correctly which can only be handled correctly from
 an extension.

 
 If you like to use this extension in relationship with Maven 3.3.1+ you
 can define the following `.mvn/extensions.xml` file:

``` xml
<extensions xmlns="http://maven.apache.org/EXTENSIONS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/EXTENSIONS/1.0.0 http://maven.apache.org/xsd/core-extensions-1.0.0.xsd">
  <extension>
    <groupId>com.soebes.maven.extensions</groupId>
    <artifactId>maven-deployer-extension</artifactId>
    <version>0.1.0</version>
  </extension>
</extensions>
```

The download from Maven Central will be done by Maven itself.
 
License
-------
[Apache License, Version 2.0, January 2004](http://www.apache.org/licenses/)


Status
------

 * Experimental
 * Not in Central

TODOs
-----

 * Proof of Concept.

