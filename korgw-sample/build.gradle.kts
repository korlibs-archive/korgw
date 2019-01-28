/*
dependencies {
    if (hasAndroid) {
        androidMainApi "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion"
        androidTestImplementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion"
    }

    commonMainApi project(":korag")
    commonMainApi project(":korev")
    commonMainApi project(":korgw")
    commonMainApi project(":korag-format")

    commonTestApi project(":korag")
    commonTestApi project(":korev")
    commonTestApi project(":korgw")
    commonTestApi project(":korag-format")

    jvmMainApi "org.jogamp.gluegen:gluegen-rt:$gluegenVersion:natives-linux-amd64"
    jvmMainApi "org.jogamp.gluegen:gluegen-rt:$gluegenVersion:natives-linux-armv6"
    jvmMainApi "org.jogamp.gluegen:gluegen-rt:$gluegenVersion:natives-linux-armv6hf"
    jvmMainApi "org.jogamp.gluegen:gluegen-rt:$gluegenVersion:natives-linux-i586"
    jvmMainApi "org.jogamp.gluegen:gluegen-rt:$gluegenVersion:natives-macosx-universal"
    jvmMainApi "org.jogamp.gluegen:gluegen-rt:$gluegenVersion:natives-windows-amd64"
    jvmMainApi "org.jogamp.gluegen:gluegen-rt:$gluegenVersion:natives-windows-i586"
    jvmMainApi "org.jogamp.jogl:jogl-all:$joglVersion:natives-linux-amd64"
    jvmMainApi "org.jogamp.jogl:jogl-all:$joglVersion:natives-linux-armv6"
    jvmMainApi "org.jogamp.jogl:jogl-all:$joglVersion:natives-linux-armv6hf"
    jvmMainApi "org.jogamp.jogl:jogl-all:$joglVersion:natives-linux-i586"
    jvmMainApi "org.jogamp.jogl:jogl-all:$joglVersion:natives-macosx-universal"
    jvmMainApi "org.jogamp.jogl:jogl-all:$joglVersion:natives-windows-amd64"
    jvmMainApi "org.jogamp.jogl:jogl-all:$joglVersion:natives-windows-i586"

    ///////////

    commonMainApi "com.soywiz:klock:$klockVersion"
    commonMainApi "com.soywiz:kmem:$kmemVersion"
    commonMainApi "com.soywiz:kds:$kdsVersion"
    commonMainApi "com.soywiz:korio:$korioVersion"

    commonMainApi "com.soywiz:korma:$kormaVersion"
    commonMainApi "com.soywiz:korim:$korimVersion"

    commonTestApi "com.soywiz:klock:$klockVersion"
    commonTestApi "com.soywiz:kmem:$kmemVersion"
    commonTestApi "com.soywiz:kds:$kdsVersion"
    commonTestApi "com.soywiz:korio:$korioVersion"

    commonTestApi "com.soywiz:korma:$kormaVersion"
    commonTestApi "com.soywiz:korim:$korimVersion"
}

//apply plugin: 'application'

ext.mainClassName = "com.soywiz.korgw.sample.KCube"

def targets = ['macosX64', 'mingwX64', 'linuxX64']

for (target in targets) {
    kotlin.targets[target].compilations.main.outputKinds("EXECUTABLE")
}

task runJvm(type: JavaExec) {
    classpath = kotlin.targets.jvm.compilations.test.runtimeDependencyFiles
    main = mainClassName
}

// runMingwX64Main, runLinuxX64Main, runMacosX64Main
for (target in targets) {
    def ctarget = target.capitalize()
    def debugExecutableTask = tasks.findByName("linkMainDebugExecutable${ctarget}")
    if (debugExecutableTask != null) {
        tasks.create("run${ctarget}", Exec) {
            dependsOn(debugExecutableTask)
            executable = kotlin.targets[target].compilations.main.getBinary("EXECUTABLE", "debug")
            args = []
        }
    }
}

tasks.create("jsWeb", Task) {
    afterEvaluate {
        dependsOn(kotlin.targets.js.compilations.test.compileKotlinTaskName, populateNodeModules)
        doLast {
            new File("$buildDir/node_modules/index.html").write("""<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="apple-mobile-web-app-capable" content="yes" />
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>Sample</title>
</head>
<body style="background:black;">

<script data-main="korgw-sample" src="https://cdnjs.cloudflare.com/ajax/libs/require.js/2.3.6/require.min.js" type="text/javascript"></script>

<script type="text/javascript">
    function toggleFullScreen() {
        var doc = window.document;
        var docEl = doc.documentElement;

        var requestFullScreen = docEl.requestFullscreen || docEl.mozRequestFullScreen || docEl.webkitRequestFullScreen || docEl.msRequestFullscreen;
        var cancelFullScreen = doc.exitFullscreen || doc.mozCancelFullScreen || doc.webkitExitFullscreen || doc.msExitFullscreen;

        if(!doc.fullscreenElement && !doc.mozFullScreenElement && !doc.webkitFullscreenElement && !doc.msFullscreenElement) {
            requestFullScreen.call(docEl);
        }
        else {
            cancelFullScreen.call(doc);
        }
    }

    window.addEventListener('touchend', function() {
        if ((!document.mozFullScreen && !document.webkitIsFullScreen)) {
            //FullScreen is disabled
            toggleFullScreen();
        } else {
            //FullScreen is enabled
        }
    });
</script>

</body>
</html>

""")
        }

    }

}

*/
