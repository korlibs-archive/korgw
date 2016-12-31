## Korui : Kotlin cORoutines User Interfaces : korio + kimage + korui for JVM, Node.JS and Browser

[![Build Status](https://travis-ci.org/soywiz/korui.svg?branch=master)](https://travis-ci.org/soywiz/korui)
[![Maven Version](https://img.shields.io/github/tag/soywiz/korui.svg?style=flat&label=maven)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22korui%22)

[KORIO](http://github.com/soywiz/korio) - [KORIM](http://github.com/soywiz/korim) - [KORUI](http://github.com/soywiz/korui)

Use with gradle:

```
compile "com.soywiz:korui:koruiVersion"
```

I'm uploading it to bintray and maven central:

For bintray:
```
maven { url "https://dl.bintray.com/soywiz/soywiz-maven" }
```

### Online Example:

![https://soywiz.github.io/korio_samples/korui1/index.html](https://soywiz.github.io/korio_samples/korui1/index.html)

![](docs/korui.png)


### HTML test:
```
npm -g install http-server
./gradlew distJs
cd korui-jtransc-example/build/jtransc-js
http-server
open http://127.0.0.1:8080
```