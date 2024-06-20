apply(from = "repositories.gradle.kts")

include(":core")
include(":extension-lib")

File(rootDir, "lib").eachDir { include("lib:${it.name}") }

File(rootDir, "extractor").eachDir { include("extractor:${it.name}") }

File(rootDir, "src").eachDir { dir ->
    dir.eachDir { subdir ->
        include("src:${dir.name}:${subdir.name}")
    }
}

fun File.eachDir(block: (File) -> Unit) {
    val files = listFiles() ?: return
    for (file in files) {
        if (
            file.isDirectory &&
            file.name != ".gradle" &&
            file.name != "build"
        ) {
            block(file)
        }
    }
}
