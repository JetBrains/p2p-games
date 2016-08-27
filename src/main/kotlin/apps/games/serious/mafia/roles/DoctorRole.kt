package apps.games.serious.mafia.roles

/**
 * Created by user on 8/24/16.
 */
class DoctorRole : PlayerRole() {
    override val role: Role
        get() = Role.DOCTOR
}