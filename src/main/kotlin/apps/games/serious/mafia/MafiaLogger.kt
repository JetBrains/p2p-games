package apps.games.serious.mafia

import apps.games.GameExecutionException
import apps.games.serious.mafia.roles.Role
import entity.User
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by user on 8/27/16.
 */

class MafiaLogger {
    private data class DayVote(val voter: User, val target: User, val day: Int)
    private data class NightEvent(val actor: Role, val target: User, val day: Int)

    private val roles = mutableMapOf<User, Role>()
    private val usersWithRole = mutableMapOf<Role, MutableList<User>>()
    private var day: Int = 1

    private val dayLogs = mutableListOf<DayVote>()
    private val nightLogs = mutableListOf<NightEvent>()
    private val calendar = Calendar.getInstance()
    private val fmt: DateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
    init {
        for(role in Role.values()){
            usersWithRole[role] = mutableListOf()
        }
    }

    /**
     * register role, held by user
     *
     * @param user - whose role to register
     * @param role - role of this user
     */
    fun registerUserRole(user: User, role: Role){
        roles[user] = role
        usersWithRole[role]!!.add(user)
    }

    /**
     * get user index among users with same role
     *
     * @param user - whose role to find
     * @return Int - index among users with same role
     * (or 0 if user is not registered yet)
     */
    fun getUserRolePosition(user: User): Int{
        for((role, users) in usersWithRole){
            if(users.contains(user)){
                return users.indexOf(user)
            }
        }
        return 0
    }

    /**
     * register day time vote
     *
     * @param voter - who voted
     * @param target - whom he chose as a terget
     */
    fun registerDayVote(voter: User, target: User){
        dayLogs.add(DayVote(voter, target, day))
    }

    fun resetDay(){
        dayLogs.removeAll { x -> x.day == day }
    }

    /**
     * Register action, that was performed at night
     *
     * @param actor - role that chose a target
     * @param target - who was chosen
     */
    fun registerNightPlay(actor: Role, target: User){
        nightLogs.add(NightEvent(actor, target, day))
    }

    /**
     * start next day log
     */
    fun nextDay(){
        day ++
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }

    /**
     * get log for daytime, that was [offset] days ago
     *
     * @param offset - number of days to rewind
     */
    fun getDayLog(offset: Int): String{
        val builder = StringBuilder()
        builder.append(formatDateWithOffset(offset) + "\n")
        val values = dayLogs.filter { x -> x.day == day - offset }
        if(values.isEmpty()){
            builder.append("THAT DAY NOTHING HAPPENED")
        }else{
            val killed = values.maxBy { x -> values.count { y -> y.target == x.target } }?.target ?:
                    throw GameExecutionException("Someone was supposed to be killed on day ${day - offset}")
            builder.append("[${killed.name}] was killed with following votes: \n\n")
            for((voter, target) in values){
                builder.append("[${voter.name}] voted against [${target.name}]\n")
            }
        }
        return builder.toString()
    }

    /**
     * get log for nighttime, that was [offset] days ago
     *
     * @param offset - number of days to rewind
     */
    fun getNightLog(offset: Int): String{
        val builder = StringBuilder()
        builder.append(formatDateWithOffset(offset) + "\n")
        val values = nightLogs.filter { x -> x.day == day - offset }
        if(values.isEmpty()){
            builder.append("THAT NIGHT NOTHING HAPPENED")
        }else{
            for((actor, target) in values){
                builder.append("[${actor.name}] chose [${target.name}] as his target\n")
            }
        }
        return builder.toString()
    }

    private fun formatDateWithOffset(offset: Int): String{
        calendar.add(Calendar.DAY_OF_YEAR, -offset)
        val res = fmt.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, offset)
        return res
    }
}