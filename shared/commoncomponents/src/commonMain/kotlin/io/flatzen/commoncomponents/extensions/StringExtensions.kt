package io.flatzen.commoncomponents.extensions

fun Any?.toNullableString(): String? = if(this == null) null else this.toString()