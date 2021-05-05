# jar-in-jar-creator
(c) 2021 anatawa12 and other contributors

A software to make mod jar smaller

## How To Use 

see output of --help.

## Licensing

This tool was released under MIT License (see LICENSE.txt).

## Third-party libraries

This software using and embeds ASM, a very small and fast Java bytecode manipulation framework,
released by ObjectWeb under BSD License(see LICENSE-asm.txt).
This library was embed to com/anatawa12/jarInJar/creator/asm directory.

This software using and embeds XZ Utils for Java, a complete implementation of XZ data compression in pure Java,
released by Tukaani Project put into public domain.
This library was embed to com/anatawa12/jarInJar/creator/xz directory.

This software using and embeds some part of Ant, a build tools,
released by The Apache Software Foundation under Apache License 2.0 
(see LICENSE-ant.txt and NOTICE-ant.txt).
This software is using and embeds `org.apache.tools.zip` package
without modification expect for relocating package names.
This library was embed to com/anatawa12/jarInJar/creator/antZip directory.
