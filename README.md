Numerus
=======

Counters, Percentiles, etc for in-memory metrics capture.

This is a utility library that started by extracting code from [Hystrix](https://github.com/Netflix/Hystrix) for use in other projects that don't need Hystrix.

## Binaries

Binaries and dependency information for Maven, Ivy, Gradle and others can be found at [http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.netflix.numerus%22).

Example for Maven:

```xml
<dependency>
    <groupId>com.netflix.numerus</groupId>
    <artifactId>numerus</artifactId>
    <version>x.y.z</version>
</dependency>
```
and for Ivy:

```xml
<dependency org="com.netflix.numerus" name="numerus" rev="x.y.z" />
```
and for Gradle:

```groovy
compile 'com.netflix.numerus:numerus:x.y.z'
```

## Build

To build:

```
$ git clone git@github.com:Netflix/Numerus.git
$ cd Numerus/
$ ./gradlew build
```


## Bugs and Feedback

For bugs, questions and discussions please use the [Github Issues](https://github.com/Netflix/Numerus/issues).

 
## LICENSE

Copyright 2014 Netflix, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
