package apps.games.serious.preferans

import apps.chat.Chat
import apps.games.GameExecutionException
import apps.games.GameManagerClass
import apps.games.primitives.protocols.DeckShuffleGame
import apps.games.primitives.protocols.RandomDeckGame
import broker.NettyGroupBroker
import crypto.random.randomInt
import entity.Group
import entity.User
import network.ConnectionManagerClass
import org.apache.log4j.BasicConfigurator
import org.bouncycastle.jce.ECNamedCurveTable
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import java.net.InetSocketAddress

/**
 * Created by user on 7/27/16.
 */

val MAX_USERS = 3
class RoundLoggerTest{

    lateinit var chats: Array<Chat>
    lateinit var groups: Array<Group>

    private val ECParams = ECNamedCurveTable.getParameterSpec("secp256k1")

    lateinit var decks: Array<ShuffledDeck>

    val DECK_SIZE = 5
    val TALON_SIZE = 2

    companion object {
        init{
            BasicConfigurator.configure()
        }
        val userClientAdresses = Array(MAX_USERS, { i -> InetSocketAddress("127.0.0.1", 1231 + 2*i) })
        val userHostAdresses = Array(MAX_USERS, { i -> InetSocketAddress("127.0.0.1", 1232 + 2*i) })

        val users = Array(MAX_USERS, { i -> User(userHostAdresses[i], "TestUser $i") })

        val connectionManagers = Array(MAX_USERS, { i -> ConnectionManagerClass(userClientAdresses[i], userHostAdresses[i]) })

        val gameManagers = Array(MAX_USERS, { i -> GameManagerClass(connectionManagers[i]) })



        @BeforeClass @JvmStatic fun setup() {
            gameManagers.forEach { x -> x.start() }
        }

        @AfterClass @JvmStatic fun teardown() {
            gameManagers.forEach { x -> x.close() }
//            connectionManagers.forEach { x -> x.close() }
        }
    }

    @Before
    fun initGame(){
        chats = Array(MAX_USERS, { i -> Mockito.mock(
                Chat::class.java) ?: throw AssertionError("Initialization error")})
        groups = Array(MAX_USERS, { i -> Group(mutableSetOf(*users)) })

        for(i in 0..MAX_USERS -1){
            chats[i].username = "TestUser $i"
            Mockito.`when`(chats[i].me()).thenReturn(users[i])
            Mockito.doReturn(
                    NettyGroupBroker(connectionManagers[i])).`when`(chats[i]).groupBroker
            Mockito.doReturn(groups[i]).`when`(chats[i]).group
        }
        val userRandomDeckGames = Array(MAX_USERS, {i -> RandomDeckGame (chats[i], groups[i], "RandomDeckForShuffle", ECParams, gameManager = gameManagers[i], deckSize = DECK_SIZE) })
        val futuresDecks = Array(MAX_USERS, {i -> gameManagers[i].initSubGame(userRandomDeckGames[i])})
        val userShuffleDeckGames = Array(MAX_USERS, {i -> DeckShuffleGame(chats[i], groups[i], "DeckShuffle", ECParams, futuresDecks[i].get(), gameManager = gameManagers[i]) })
        val futureShuffledDecks = Array(MAX_USERS, {i -> gameManagers[i].initSubGame(userShuffleDeckGames[i])})
        decks = Array(MAX_USERS, {i -> ShuffledDeck(futuresDecks[i].get(), futureShuffledDecks[i].get()) })
        //decrypt all decks
        for(deck in decks){
            for(otherDeck in decks){
                deck.encrypted.deck.decryptSeparate(otherDeck.encrypted.keys)
            }
        }
    }


    /**
     * Test, that hash of all registered keys is computed correclty
     */
    @Test
    fun testTestKeyHashes(){
        val logger = RoundLogger(MAX_USERS, DECK_SIZE, TALON_SIZE)
        for(i in 0..MAX_USERS-1){
            assertNull(logger.getUserKeysHash(i))
        }
        for(i in 0..MAX_USERS-1){
            for(j in 0..DECK_SIZE-1){
                logger.registerCardKey(i, j, decks[i].encrypted.keys[j])
            }
        }
        for(i in 0..MAX_USERS-1){
            for(j in 0..MAX_USERS-1){
                assertEquals(decks[i].encrypted.hashes[users[j]], logger.getUserKeysHash(j))
            }
        }
    }


    /**
     * Tets talon inference and game logging on normal games
     */
    @Test
    fun testTalon(){
        val logger = RoundLogger(MAX_USERS, DECK_SIZE, TALON_SIZE)
        logger.updateBet(Bet.NINE_NO_TRUMP)
        val talon = mutableSetOf<Int>()
        while (talon.size != TALON_SIZE){
            val card = randomInt(DECK_SIZE)
            talon.add(card)
        }
        var playerId = 0
        val unplayed = (0..DECK_SIZE-1).minus(talon).toMutableList()
        while (unplayed.size != 0){
            val pos = randomInt(unplayed.size)
            val card = unplayed.removeAt(pos)
            val next = logger.registerPlay(playerId to card) ?: fail()
            playerId = (next as Int)
        }
        assertTrue(logger.verifyRoundPlays())
        assertEquals(talon.sorted(), logger.getDiscardedTalon())
    }

    /**
     * Test case for scenario where everybody passed
     */
    @Test
    fun testPasses(){
        val logger = RoundLogger(MAX_USERS, DECK_SIZE, TALON_SIZE)
        logger.updateBet(Bet.PASS)
        val talon = mutableSetOf<Int>()
        while (talon.size != TALON_SIZE){
            val card = randomInt(DECK_SIZE)
            talon.add(card)
        }
        logger.registerTalon(talon.toList())
        var playerId = 0
        val unplayed = (0..DECK_SIZE-1).minus(talon).toMutableList()
        while (unplayed.size != 0){
            val pos = randomInt(unplayed.size)
            val card = unplayed.removeAt(pos)
            val next = logger.registerPlay(playerId to card) ?: fail()
            playerId = (next as Int)
        }
        assertTrue(logger.verifyRoundPlays())
    }
}