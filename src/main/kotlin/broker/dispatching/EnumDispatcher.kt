package broker.dispatching

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import com.google.common.collect.SetMultimap
import com.google.protobuf.GeneratedMessage
import proto.GenericMessageProto
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

/**
 * Created by user on 6/21/16.
 */
//TODO - better way to dispatch
/**
 * Dispatch messages for given type
 * No simple way to check function parameter types:
 * only type enum is checked. Whatever is stored by that
 * value - passed into function
 */
class EnumDispatcher<T: GeneratedMessage, E: Enum<E>>: Dispatcher {
    val listeners: Map<E, MutableList<KFunction<GenericMessageProto.GenericMessage>>>

    constructor(enumcls: Class<E>){
        val values = enumcls.enumConstants
        val entries: MutableList<Pair<E, MutableList<KFunction<GenericMessageProto.GenericMessage>>>> = mutableListOf()
        for(x in values){
            val tmp: MutableList<KFunction<GenericMessageProto.GenericMessage>> = mutableListOf()
            entries.add(Pair(x, tmp))
        }
        listeners = mapOf(*entries.toTypedArray())
    }

    fun dispatch(msg: T){
        print("wololo")
        //(msg.getField(msg.descriptorForType.findFieldByName("type")) as Descriptors.EnumValueDescriptor).number
        //
        /*
        TODO PLAN:
        1)Add constraints on protobuff: type always first
        2)Add constraints on protobuff: type value = field value
        3)by msg get TYPE index
        4)by type index get FIELD[index] as VALUE
        5)Dispatch to listeners[TYPE](FIELD[index])
        ETA - tommorow
         */
    }
}