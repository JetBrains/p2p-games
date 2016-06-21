package broker.dispatching

/**
 * Created by user on 6/21/16.
 */
interface  Dispatcher{

    fun <E: Enum<E>> dispatch(value: E){
        print(value)
    }
}

