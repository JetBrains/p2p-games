package apps.games.serious.mafia

import apps.games.GameExecutionException
import apps.games.serious.mafia.roles.Role
import apps.games.serious.mafia.subgames.sum.SMSfAResult
import entity.User
import java.math.BigInteger
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by user on 8/27/16.
 */

class MafiaLogger {
    private data class DayVote(val voter: User, val target: User, val day: Int)
    private data class NightEvent(val actor: Role, val target: User, val day: Int)
    private data class DetectiveEvent(val target: User, val isMafia: Boolean, val day: Int)

    private val roles = mutableMapOf<User, Role>()
    private val usersWithRole = mutableMapOf<Role, MutableList<User>>()
    private var day: Int = 0

    private val dayLogs = mutableListOf<DayVote>()
    private val nightLogs = mutableListOf<NightEvent>()
    private val detectiveLogs = mutableListOf<DetectiveEvent>()

    private val doctorChoicesSMS = mutableListOf<SMSfAResult>()
    private val detectiveChoicesSMS = mutableListOf<SMSfAResult>()
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

    /**
     * reset all plays, that took part in day phase
     * of current day
     */
    fun resetDay(){
        dayLogs.removeAll { x -> x.day == day }
    }

    /**
     * register SMSfAResult - intermediate result
     * for doctor picking his target
     */
    fun registerDoctorFirstPhase(smsfa: SMSfAResult){
        doctorChoicesSMS.add(smsfa)
    }

    /**
     * get random input for last detective choice
     */
    fun getDoctorNoisedInput(): String{
        return doctorChoicesSMS.last().salt + " " + doctorChoicesSMS.last().R.toString()
    }

    /**
     * validate R values for last round. If something
     * is not correct - return false
     *
     * @return true - if hashes ar OK, false otherwise
     */
    fun verifyLastDoctorRHashes(hashes: Map<User, String>): Boolean{
        return hashes == doctorChoicesSMS.last().RHashes
    }

    /**
     * get sum for last doctor choice
     */
    fun getDoctorSum(): BigInteger{
        return doctorChoicesSMS.last().sum
    }

    /**
     * register SMSfAResult - result of detective
     * picking his target
     */
    fun registerDetectiveChoiceSMS(smsfa: SMSfAResult){
        detectiveChoicesSMS.add(smsfa)
    }

    /**
     * get random input for last detective choice
     */
    fun getDetectiveNoisedInput(): String{
        return detectiveChoicesSMS.last().salt + " " + detectiveChoicesSMS.last().R.toString()
    }

    /**
     * get sum for last detective choice
     */
    fun getDetectiveSum(): BigInteger{
        return detectiveChoicesSMS.last().sum
    }

    /**
     * validate R values for last round. If something
     * is not correct - return false
     *
     * @return true - if hashes ar OK, false otherwise
     */
    fun verifyLastDetectiveRHashes(hashes: Map<User, String>): Boolean{
        return hashes == detectiveChoicesSMS.last().RHashes
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
     * register play by detective to be displayed in logs
     *
     * @param target - user, that was checked
     * @param isMafia - whether user was mafia, or not
     */
    fun registerDetectivePlay(target: User, isMafia: Boolean){
        detectiveLogs.add(DetectiveEvent(target, isMafia, day))
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
            builder.append("That day nothing happened")
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

        val values = nightLogs.filter { x -> x.day == day - offset }
        val detectivePlay = detectiveLogs.firstOrNull { x -> x.day == day - offset }
        //TODO

        if (detectivePlay != null) {
            builder.append("You as a detective checked [${detectivePlay.target.name}] \n and he is ")
            if (!detectivePlay.isMafia){
                builder.append(" NOT ")
            }
            builder.append("Mafia \n\n\n")
        }
        for((actor, target) in values){
            builder.append("[${actor.name}] chose [${target.name}] as his target\n")
        }
        if(builder.isEmpty()){
            builder.append("That night nothing happened")
        }
        builder.insert(0, formatDateWithOffset(offset) + "\n")
        return builder.toString()
    }

    /**
     * create a readable date string for given offset
     *
     * @param offset - days to rewind
     * @return String - formated date string
     */
    private fun formatDateWithOffset(offset: Int): String{
        calendar.add(Calendar.DAY_OF_YEAR, -offset)
        val res = fmt.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, offset)
        return res
    }
}