# Ant Colony Optimization (ACO) Algorithm

Project developed for the IF14Y010 class in 2021-2022 academic year in Université de Paris

## Prerequisites

This project is developed by Oracle Java SE `17.0.2` and Gradle `7.3`

Due to [JEP 403](https://openjdk.java.net/jeps/403) please add following JVM variables when building with Gradle:
```sh
--add-exports
jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
--add-exports
jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
--add-exports
jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
--add-exports
jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
--add-exports
jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
```
You can also use `LINT-BUILD-AND-RUN` run configuration if you're using IntelliJ as IDE.

## File structure

This project composed by two modules:

- [`core`](./core): contains the source code and logic of the ACO
- [`gui`](./gui): contains the source code and logic of graphical user interface (GUI).

A more detailed readme can be found under the each module's folder.

## Development

To build all modules:
```shell
gradle build
```

**IMPORTANT:** We're using Spotless with Google Java Format code conventions as Linter. Sometimes the build can fail due to lint issues.

To fix all linter errors:
```shell
gradle spotlessApply
```
