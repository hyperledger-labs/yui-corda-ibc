package jp.datachain.corda.ibc.cosmos

@Target(AnnotationTarget.CLASS)
annotation class ReqPath(val pathFormat: String)

fun ReqPath.path(vararg args: String) = pathFormat.format(*args)
