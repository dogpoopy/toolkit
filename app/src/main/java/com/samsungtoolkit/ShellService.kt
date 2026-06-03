package com.samsungtoolkit

class ShellService : IShellService.Stub() {
    override fun run(command: String): String = try {
        val proc = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
        val out = proc.inputStream.bufferedReader().readText().trim()
        proc.waitFor()
        out
    } catch (_: Exception) { "" }
}
