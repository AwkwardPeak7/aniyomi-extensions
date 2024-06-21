apply(from = "repositories.gradle.kts")

include(":project:core")
include(":project:extension-lib")
include(":project:common-lib")

File(rootDir, "lib").eachDir { include("lib:${it.name}") }

File(rootDir, "extractor").eachDir { include("extractor:${it.name}") }


if (System.getenv("CI") != "true") {
    // Local development (full project build)

    File(rootDir, "src").eachDir { dir ->
        dir.eachDir { subdir ->
            include("src:${dir.name}:${subdir.name}")
        }
    }
} else {
    // Running in CI (GitHub Actions)

    val chunkSize = System.getenv("CI_CHUNK_SIZE").toInt()
    val chunk = System.getenv("CI_CHUNK_NUM").toInt()

    // Loads individual extensions
    File(rootDir, "src").getChunk(chunk, chunkSize)?.forEach {
        include( "src:${it.parentFile.name}:${it.name}")
    }
}

fun File.getChunk(chunk: Int, chunkSize: Int): List<File>? {
    return listFiles()
        ?.filter { it.isDirectory }
        ?.mapNotNull { dir -> dir.listFiles()?.filter { it.isDirectory } }
        ?.flatten()
        ?.sortedBy { it.name }
        ?.chunked(chunkSize)
        ?.toList()
        ?.get(chunk)
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
