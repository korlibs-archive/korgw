<p align="center">
    <img alt="Korio" src="https://raw.githubusercontent.com/korlibs/korlibs-logos/master/128/korui.png" />
</p>

<h2 align="center">Korui</h2>

<p align="center">
    With Korui you can create your UI once, and run it in HTML5, Java AWT and Android.
</p>

<!-- BADGES -->
<p align="center">
	<a href="https://github.com/korlibs/korgw/actions"><img alt="Build Status" src="https://github.com/korlibs/korgw/workflows/CI/badge.svg" /></a>
	<a href="https://bintray.com/korlibs/korlibs/korgw"><img alt="Maven Version" src="https://img.shields.io/bintray/v/korlibs/korlibs/korgw.svg?style=flat&label=maven" /></a>
	<a href="https://discord.korge.org/"><img alt="Discord" src="https://img.shields.io/discord/728582275884908604?logo=discord" /></a>
</p>
<!-- /BADGES -->

<!-- SUPPORT -->
<h2 align="center">Support korgw</h2>
<p align="center">
If you like korgw, or want your company logo here, please consider <a href="https://github.com/sponsors/soywiz">becoming a sponsor ★</a>,<br />
in addition to ensure the continuity of the project, you will get exclusive content.
</p>
<!-- /SUPPORT -->

[All KOR libraries](https://github.com/soywiz/kor)

[KORIO](http://github.com/soywiz/korio) - [KORIM](http://github.com/soywiz/korim) - [KORUI](http://github.com/soywiz/korui)

Use with gradle:

```
compile "com.soywiz:korui:$korVersion"
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
