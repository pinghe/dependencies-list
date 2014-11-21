dependencies-list
====

Karaf bundle dependencies list Command

 dependencies-list - list bundle dependencies.

Description:

 Karaf command to display a flat list of Bundle dependencies
 as wired in the container.

Building from source:
===

To build, invoke:
 
 mvn install

dependencies-list installation:
===

To install in Karaf, invoke from console:

 install -s mvn:com.savoirtech.karaf.commands/dependencies-list


To execute command on Karaf, invoke:

 aetos:dependencies-list BundleID

