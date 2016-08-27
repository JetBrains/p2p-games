package apps.games.serious.mafia.roles

/**
 * Created by user on 8/24/16.
 */
class MafiaRole : PlayerRole() {
    override val role: Role
        get() = Role.MAFIA
}