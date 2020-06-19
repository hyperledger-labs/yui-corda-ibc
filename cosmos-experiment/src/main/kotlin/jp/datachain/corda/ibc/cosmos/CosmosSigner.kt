package jp.datachain.corda.ibc.cosmos

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import jp.datachain.amino.DisfixWrapper
import java.io.File

data class CosmosSigner(val from: String, val chainID: String, val keyringBackend: String) {
    val mapper = ObjectMapper().registerKotlinModule()

    operator fun invoke(tx: DisfixWrapper) : DisfixWrapper {
        val unsignedJson = mapper.writeValueAsString(tx)

        val proc = ProcessBuilder("gaiacli tx sign - --from $from --chain-id $chainID --keyring-backend $keyringBackend".split(" "))
                .start()

        val stdin = proc.outputStream.bufferedWriter()
        stdin.write(unsignedJson)
        stdin.close()

        val stdout = proc.inputStream.bufferedReader()
        val signedJson = stdout.readText()
        stdout.close()

        proc.waitFor()

        return mapper.readValue(signedJson, DisfixWrapper::class.java)
    }
}