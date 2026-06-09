package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class BackupBlacklistedPage(
    @ProtoNumber(1) val id: String,
    @ProtoNumber(2) val hash: ByteArray,
    @ProtoNumber(3) val thumbnail: ByteArray,
    @ProtoNumber(4) val createdAt: Long,
)
