import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.plugins.ide.idea.model.*

apply(plugin = "idea")

if (Os.isFamily(Os.FAMILY_UNIX) &&
    (File("/.dockerenv").exists() || System.getenv("TRAVIS") != null || System.getenv("GITHUB_REPOSITORY") != null) &&
    (File("/usr/bin/apt-get").exists())
) {

    exec { commandLine("sudo", "apt-get", "update")  }
    exec { commandLine("sudo", "apt-get", "-y", "install", "freeglut3-dev")  }
    //exec { commandLine("sudo", "apt-get", "-y", "install", "libgtk-3-dev") }
}

val idea: IdeaModel by project

idea.module {
    excludeDirs = LinkedHashSet()
    for (name in listOf("old", "@old", "nlib", "gradle")) {
        excludeDirs.add(File(rootProject.rootDir, name))
    }
}
