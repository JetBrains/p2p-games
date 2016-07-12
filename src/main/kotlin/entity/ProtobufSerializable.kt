package entity

import com.google.protobuf.GeneratedMessage

/**
 * Created by user on 6/24/16.
 */
interface ProtobufSerializable<T : GeneratedMessage> {
    fun getProto(): T
}