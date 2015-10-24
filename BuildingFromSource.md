# Introduction #
The source repository contains many modules, as explained in RepositoryStructure.

The default product that can be built from the sources is a basic Passerelle model editor packaged as an eclipse RCP application.

We used to provide regularly updated pre-packaged binaries for this, but we've stopped this when Google closed down the usage of the Downloads page.

To obtain a binary package you can either contact us via the forum or build your own.

This document explains how to configure your own build.

# Details #

## Prerequisites ##
To get a working build configuration you need :
  * A [JDK 1.7](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
  * [Maven 3](http://maven.apache.org)
  * A SVN client, e.g. [TortoiseSVN](http://tortoisesvn.net/downloads.html)

In your maven settings (typically located in an .m2 folder in your user's home directory), you need to configure public read-access to iSencia's Nexus repository. Please contact us via the forum for more info.

## Procedure ##
Once you've got everything set up, the approach is quite straightforward :
  1. check out all sources from the trunk in one shot in your selected local directory
  1. find the project/directory `com.isencia.passerelle.parent` and open a command prompt in there
  1. run `mvn clean package`
  1. have some patience... while dependencies are downloaded, bundles get compiled, tests are executing and finally... the workbench packages are built...
  1. finally, you should find the packages in : `passerelle-eclipse-workbench/com.isencia.passerelle.workbench.product/target/products`