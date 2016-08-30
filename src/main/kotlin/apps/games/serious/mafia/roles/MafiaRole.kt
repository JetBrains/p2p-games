package apps.games.serious.mafia.roles

/**
 * Created by user on 8/24/16.
 */
class MafiaRole : PlayerRole() {
    override val role: Role
        get() = Role.MAFIA

    companion object{
        val MESSAGE_INPUT_TIMEOUT: Long = 30
        val TARGET_CHOICE_TIMEOUT: Long = 30
    }
}