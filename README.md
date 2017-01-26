## Korui : Kotlin cORoutines User Interfaces : korio + kimage + korui for JVM, Kotlin-JS, Android, Jtransc+Browser

With Korui you can create your UI once, and run it in HTML5, Java AWT and Android.

![](https://raw.githubusercontent.com/soywiz/kor/master/logos/128/korui.png)

[All KOR libraries](https://github.com/soywiz/kor)

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

[https://soywiz.github.io/korio_samples/korui1/index.html](https://soywiz.github.io/korio_samples/korui1/index.html)

![](docs/android.png)

![](docs/korui.png)


### HTML test:
```
npm -g install http-server
./gradlew distJs
cd korui-jtransc-example/build/jtransc-js
http-server
open http://127.0.0.1:8080
```