import com.soywiz.klock.DateTime
import com.soywiz.klock.parse
import dev.brella.blasement.bindAs
import dev.brella.blasement.bindAsNullable
import dev.brella.blasement.common.events.BlaseballFanTrackers
import dev.brella.blasement.common.events.BlasementFanDatabasePayload
import dev.brella.blasement.common.events.FanID
import dev.brella.blasement.common.events.TimeRange
import dev.brella.blasement.common.getJsonObjectOrNull
import dev.brella.blasement.common.getStringOrNull
import dev.brella.blasement.common.uuidOrNull
import dev.brella.kornea.blaseball.BlaseballApi
import dev.brella.kornea.blaseball.base.common.BLASEBALL_TIME_PATTERN
import dev.brella.kornea.blaseball.base.common.EnumBlaseballSnack
import dev.brella.kornea.blaseball.base.common.PlayerID
import dev.brella.kornea.blaseball.base.common.TeamID
import dev.brella.kornea.blaseball.base.common.beans.BlaseballDatabaseGame
import dev.brella.kornea.blaseball.chronicler.ChroniclerApi
import dev.brella.kornea.blaseball.chronicler.EnumOrder
import dev.brella.ktornea.common.getAsResult
import io.jsonwebtoken.JwtBuilder
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import io.ktor.client.*
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.Option
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import org.bouncycastle.util.encoders.Hex
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.await
import org.springframework.r2dbc.core.awaitSingleOrNull
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import java.io.File
import java.security.SecureRandom
import java.time.Duration
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue
import kotlin.time.milliseconds
import org.springframework.r2dbc.core.bind as bindNullable

import java.util.UUID as JUUID
import dev.brella.kornea.blaseball.base.common.UUID as KUUID


class TheBlasement(val json: Json, val httpClient: HttpClient, val blaseballApi: BlaseballApi, val chroniclerApi: ChroniclerApi) : CoroutineScope {
    companion object {
        const val COOKIE_NAME = "connect.sid"
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Default

    val random = SecureRandom.getInstanceStrong()
    val passwords = Argon2PasswordEncoder()

    val liveData = LiveData(blaseballApi, chroniclerApi, this)
//    val globalFeed = BlaseballFeed.Global(this, range = runBlocking { dateTimeRangeForSeason(0) })

//    val globalEventFeed = BlasementEventFeed(globalFeed.flow, this)

    val fans: MutableMap<KUUID, BlasementHostFan> = HashMap()
    //2021-03-01T16:00:13.446802Z

//    val trace = globalFeed.flow.onEach { event ->
//        println("[${event.event.created}] TRACE: $event")
//    }.launchIn(this)

    suspend fun dateTimeRangeForSeason(season: Int): TimeRange {
        if (liveData.date.let { it == null || it.season == season }) {
            val simData = blaseballApi.getSimulationData().get()

            if (simData.season == season) return TimeRange(simData.godsDayDate, simData.electionDate)
        }

        if (season < 11) {
            val startingTime = chroniclerApi.getGames(
                order = EnumOrder.ASC,
                season = season,
                count = 1
            ).get().first().startTime

            val endTime = chroniclerApi.getGames(
                order = EnumOrder.DESC,
                season = season,
                count = 1
            ).get().first().endTime

            return TimeRange.fromChronicler(startingTime, endTime)
        } else {
            val startingTime = chroniclerApi.getGames(
                order = EnumOrder.ASC,
                season = season,
                count = 1
            ).get().first().startTime

            println("Starting: $startingTime")

            val simulationData = httpClient.getAsResult<JsonObject>("https://api.sibr.dev/chronicler/v2/entities?type=sim&at=${startingTime}&count=1")
                .get()
                .also { println(it) }
                .getValue("items")
                .jsonArray
                .first()
                .jsonObject
                .getValue("data")
                .jsonObject

            return TimeRange.fromChronicler(
                simulationData.getValue("godsDayDate").jsonPrimitive.content,
                simulationData.getValue("electionDate").jsonPrimitive.content
            )
        }
    }

//    var connectionFactory: ConnectionFactory = ConnectionFactories.get("r2dbc:h2:mem:///test?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
//
//    var client: DatabaseClient = DatabaseClient.create(connectionFactory)

    val configJson: JsonObject? = File("blasement.json").takeIf(File::exists)?.readText()?.let(Json::decodeFromString)
    val r2dbcConfig: JsonObject = configJson?.getJsonObjectOrNull("r2dbc") ?: File("r2dbc.json").readText().let(Json::decodeFromString)

    val connectionFactory: ConnectionFactory = ConnectionFactories.get(
        r2dbcConfig.run {
            val builder = getStringOrNull("url")?.let { ConnectionFactoryOptions.parse(it).mutate() }
                          ?: ConnectionFactoryOptions.builder().option(ConnectionFactoryOptions.DRIVER, "pool").option(ConnectionFactoryOptions.PROTOCOL, "postgresql")

            getStringOrNull("connectTimeout")
                ?.let { builder.option(ConnectionFactoryOptions.CONNECT_TIMEOUT, Duration.parse(it)) }

            getStringOrNull("database")
                ?.let { builder.option(ConnectionFactoryOptions.DATABASE, it) }

            getStringOrNull("driver")
                ?.let { builder.option(ConnectionFactoryOptions.DRIVER, it) }

            getStringOrNull("host")
                ?.let { builder.option(ConnectionFactoryOptions.HOST, it) }

            getStringOrNull("password")
                ?.let { builder.option(ConnectionFactoryOptions.PASSWORD, it) }

            getStringOrNull("port")?.toIntOrNull()
                ?.let { builder.option(ConnectionFactoryOptions.PORT, it) }

            getStringOrNull("protocol")
                ?.let { builder.option(ConnectionFactoryOptions.PROTOCOL, it) }

            getStringOrNull("ssl")?.toBoolean()
                ?.let { builder.option(ConnectionFactoryOptions.SSL, it) }

            getStringOrNull("user")
                ?.let { builder.option(ConnectionFactoryOptions.USER, it) }

            getJsonObjectOrNull("options")?.forEach { (key, value) ->
                val value = value as? JsonPrimitive ?: return@forEach
                value.longOrNull?.let { builder.option(Option.valueOf(key), it) }
                ?: value.doubleOrNull?.let { builder.option(Option.valueOf(key), it) }
                ?: value.booleanOrNull?.let { builder.option(Option.valueOf(key), it) }
                ?: value.contentOrNull?.let { builder.option(Option.valueOf(key), it) }
            }

            builder.build()
        }
    )

    val client = DatabaseClient.create(connectionFactory)


//    val connectionFactory = H2ConnectionFactory(
//        H2ConnectionConfiguration.builder()
//            .file("./blasement")
//            .property(H2ConnectionOption.DB_CLOSE_DELAY, "-1")
//            .property(H2ConnectionOption.AUTO_SERVER, "true")
//            .build()
//    )

    val blasementInitialisationJob = launch {

        try {
            client.sql("CREATE TABLE IF NOT EXISTS fans (fan_id uuid NOT NULL, email VARCHAR(128), apple_id VARCHAR(128), google_id VARCHAR(128), facebook_id VARCHAR(128), name VARCHAR(128), password VARCHAR(128), coins BIGINT NOT NULL DEFAULT 250, last_active VARCHAR(128) NOT NULL, created VARCHAR(128) NOT NULL, login_streak INT NOT NULL DEFAULT 0, idol uuid, favourite_team uuid, has_unlocked_shop BOOLEAN NOT NULL DEFAULT FALSE, has_unlocked_elections BOOLEAN NOT NULL DEFAULT FALSE, peanuts_eaten INT NOT NULL DEFAULT 0, squirrels INT NOT NULL DEFAULT 0, light_mode BOOLEAN NOT NULL DEFAULT false, inventory_space INT NOT NULL DEFAULT 8, spread VARCHAR(128) NOT NULL DEFAULT '', coffee INT, fav_number INT, read_only BOOLEAN NOT NULL DEFAULT false);")
                .await()

            client.sql("CREATE TABLE IF NOT EXISTS items (id BIGSERIAL PRIMARY KEY, fan_id uuid NOT NULL, item_name VARCHAR(64) NOT NULL, quantity INT NOT NULL DEFAULT 0);")
                .await()

            client.sql("CREATE TABLE IF NOT EXISTS bets (id BIGSERIAL PRIMARY KEY, fan_id uuid NOT NULL, game_id uuid NOT NULL, team_id uuid NOT NULL, amount INT NOT NULL);")
                .await()

            client.sql("CREATE TABLE IF NOT EXISTS trackers (fan_id uuid NOT NULL, begs INT NOT NULL DEFAULT 0, bets INT NOT NULL DEFAULT 0, votes_cast INT NOT NULL DEFAULT 0, snacks_bought INT NOT NULL DEFAULT 0, snack_upgrades INT NOT NULL DEFAULT 0);")
                .await()

            client.sql("CREATE TABLE IF NOT EXISTS toasts (id BIGSERIAL PRIMARY KEY, fan_id uuid NOT NULL, toast VARCHAR (256) NOT NULL, timestamp BIGINT NOT NULL)")
                .await()

            client.sql("SELECT fan_id, email, apple_id, google_id, facebook_id, name, password, coins, last_active, created, login_streak, idol, favourite_team, has_unlocked_shop, has_unlocked_elections, peanuts_eaten, squirrels, light_mode, inventory_space, spread, coffee, fav_number FROM fans;")
                .map { row ->
                    BlasementFanDatabasePayload(
                        FanID((row["fan_id"] as? JUUID)?.kornea ?: (row["fan_id"] as? String)?.uuidOrNull() ?: throw IllegalArgumentException("No fan_id found for $row")),
                        row["email"] as? String,
                        row["apple_id"] as? String,
                        row["google_id"] as? String,
                        row["facebook_id"] as? String,
                        row["name"] as? String,
                        row["password"] as? String,
                        row["coins"] as Long,
                        BLASEBALL_TIME_PATTERN.parse(row["last_active"] as String),
                        BLASEBALL_TIME_PATTERN.parse(row["created"] as String),
                        row["login_streak"] as Int,
                        (row["idol"] as? JUUID)?.kornea?.let(::PlayerID),
                        (row["favourite_team"] as? JUUID)?.kornea?.let(::TeamID),
                        row["has_unlocked_shop"] as Boolean,
                        row["has_unlocked_elections"] as Boolean,
                        row["peanuts_eaten"] as Int,
                        row["squirrels"] as Int,
                        row["light_mode"] as Boolean,
                        row["inventory_space"] as Int,
                        (row["spread"] as? String)?.split(',')?.map(String::trim)?.mapNotNull(String::toIntOrNull) ?: emptyList(),
                        row["coffee"] as? Int,
                        row["fav_number"] as? Int
                    )
                }.all()
                .asFlow()
                .collect { databasePayload ->
                    val fanID = databasePayload.fanID.uuid

                    val items =
                        client.sql("SELECT item_name, quantity FROM items WHERE fan_id = $1")
                            .bindAs("$1", fanID)
                            .map { row, _ ->
                                val name = row["item_name"] as String
                                val item = EnumBlaseballSnack.values().firstOrNull { it.name == name }
                                           ?: return@map null

                                Pair(item, (row["quantity"] as Number).toInt())
                            }.all().collectList().awaitFirstOrNull() ?: emptyList()


                    val bets =
                        client.sql("SELECT game_id, team_id, amount FROM bets WHERE fan_id = $1")
                            .bindAs("$1", fanID)
                            .map { row, _ ->
                                (row["game_id"] as? JUUID)?.kornea?.let { gameID ->
                                    (row["team_id"] as? JUUID)?.kornea?.let { teamID ->
                                        (row["amount"] as? Number)?.toInt()?.let { amount ->
                                            Triple(gameID, teamID, amount)
                                        }
                                    }
                                }
                            }.all().collectList().awaitFirstOrNull()?.filterNotNull() ?: emptyList()

                    val trackers =
                        client.sql("SELECT begs, bets, votes_cast, snacks_bought, snack_upgrades FROM trackers WHERE fan_id = $1")
                            .bindAs("$1", fanID)
                            .map { row, _ ->
                                BlaseballFanTrackers(
                                    row["begs"] as Int,
                                    row["bets"] as Int,
                                    row["votes_cast"] as Int,
                                    row["snacks_bought"] as Int,
                                    row["snack_upgrades"] as Int
                                )
                            }.awaitSingleOrNull() ?: BlaseballFanTrackers()

                    fans[fanID] = BlasementHostFan(databasePayload, this@TheBlasement, items, bets, trackers)
                }
        } catch (th: Throwable) {
            th.printStackTrace()
            throw th
        }
    }

    /** Sub Event Feeds */

    private val gamesToday = ValueCache { date: BlaseballDate -> blaseballApi.getGamesByDate(date.season, date.day).get() }
    private val gamesTomorrow = ValueCache { date: BlaseballDate -> blaseballApi.getGamesByDate(date.season, date.day + 1).get() }

    val privateKey = File("private.key").let { file ->
        if (file.exists()) RSAPrivateKey(file.readBytes())
        else {
            val key = Keys.keyPairFor(SignatureAlgorithm.RS512)
            file.writeBytes(key.private.encoded)

            File("public.key").writeBytes(key.public.encoded)

            File("public.auth").writeText(Hex.toHexString(key.public.encoded))

            key.private
        }
    }
    val parser = Jwts.parserBuilder().requireIssuer("3de20b85-df54-4894-8d23-057796cd1a3b").setSigningKey(privateKey).build()

    inline fun jwt(builder: JwtBuilder.() -> Unit) =
        Jwts.builder()
            .setIssuer("3de20b85-df54-4894-8d23-057796cd1a3b")
            .signWith(privateKey)
            .apply(builder)
            .compact()

    suspend fun today(): BlaseballDate =
        liveData.date ?: blaseballApi.getSimulationData().get().run { BlaseballDate(season, day) }

    suspend fun gamesToday(): List<BlaseballDatabaseGame> =
        gamesToday.get(today())

    suspend fun gamesTomorrow(): List<BlaseballDatabaseGame> =
        gamesTomorrow.get(today())

    suspend fun newFan(fanID: JUUID, name: String?, favouriteTeam: TeamID) = newFan(fanID.kornea, name, favouriteTeam)
    suspend fun newFan(fanID: KUUID, name: String?, favouriteTeam: TeamID): Pair<String, BlasementHostFan> {
        val authToken = jwt {
            if (name != null) setSubject(name)
            setId(fanID.toString())
        }

        val now = DateTime.now().utc

        val fan = BlasementHostFan(
            blasement = this,
            id = FanID(fanID),
            name = name,
            email = "$name@example.com",
            coins = 250,
            favouriteTeam = favouriteTeam,

            lastActive = now,
            created = now,

            loginStreak = 0
        )

        fan.apply {
            setItemQuantity(EnumBlaseballSnack.VOTES) { 1 }
            setItemQuantity(EnumBlaseballSnack.SNAKE_OIL) { 1 }
            setItemQuantity(EnumBlaseballSnack.PEANUTS) { 10 }
        }

        try {
            client.sql("INSERT INTO fans (fan_id, email, apple_id, google_id, facebook_id, name, password, last_active, created, coins, idol, favourite_team, has_unlocked_shop, has_unlocked_elections, coffee, fav_number) VALUES ( $1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16 )")
                .bindAs("$1", fan.id)
                .bindNullable("$2", fan.email)
                .bindNullable("$3", fan.appleId)
                .bindNullable("$4", fan.googleId)
                .bindNullable("$5", fan.facebookId)
                .bindNullable("$6", fan.name)
                .bindNullable("$7", fan.password)
                .bind("$8", BLASEBALL_TIME_PATTERN.format(fan.lastActive))
                .bind("$9", BLASEBALL_TIME_PATTERN.format(fan.created))
                .bind("$10", fan.coins)
                .bindAsNullable("$11", fan.idol)
                .bindAsNullable("$12", fan.favouriteTeam)
                .bind("$13", fan.hasUnlockedShop)
                .bind("$14", fan.hasUnlockedElections)
                .bindNullable<Int>("$15", null)
                .bindNullable<Int>("$16", null)
                .await()
        } catch (th: Throwable) {
            th.printStackTrace()
            throw th
        }

        fans[fanID] = fan
        return Pair(authToken, fan)
    }

    suspend fun newFanWithEmail(fanID: JUUID, email: String, password: String) = newFanWithEmail(fanID.kornea, email, password)
    suspend fun newFanWithEmail(fanID: KUUID, email: String, password: String): Pair<String, BlasementHostFan> {
        val authToken = jwt {
            setSubject(email)
            setId(fanID.toString())
        }

        val now = DateTime.now().utc

        val fan = BlasementHostFan(
            blasement = this,
            id = FanID(fanID),
            email = email,
            password = password,
            coins = 250,
            favouriteTeam = null,

            lastActive = now,
            created = now,

            loginStreak = 0
        )

        fan.apply {
            setItemQuantity(EnumBlaseballSnack.VOTES) { 1 }
            setItemQuantity(EnumBlaseballSnack.SNAKE_OIL) { 1 }
            setItemQuantity(EnumBlaseballSnack.PEANUTS) { 10 }

            setItemQuantity(EnumBlaseballSnack.FLUTES) { 10 }
        }

        try {
            client.sql("INSERT INTO fans (fan_id, email, apple_id, google_id, facebook_id, name, password, last_active, created, coins, idol, favourite_team, has_unlocked_shop, has_unlocked_elections, coffee, fav_number) VALUES ( $1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16 )")
                .bindAs("$1", fanID)
                .bindNullable("$2", fan.email)
                .bindNullable("$3", fan.appleId)
                .bindNullable("$4", fan.googleId)
                .bindNullable("$5", fan.facebookId)
                .bindNullable("$6", fan.name)
                .bindNullable("$7", fan.password)
                .bind("$8", BLASEBALL_TIME_PATTERN.format(fan.lastActive))
                .bind("$9", BLASEBALL_TIME_PATTERN.format(fan.created))
                .bind("$10", fan.coins)
                .bindAsNullable("$11", fan.idol)
                .bindNullable<JUUID>("$12", null)
                .bind("$13", fan.hasUnlockedShop)
                .bind("$14", fan.hasUnlockedElections)
                .bindNullable<Int>("$15", null)
                .bindNullable<Int>("$16", null)
                .await()
        } catch (th: Throwable) {
            th.printStackTrace()
            throw th
        }

        fans[fanID] = fan
        return Pair(authToken, fan)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun login(email: String, password: String): Pair<String, BlasementHostFan>? {
        val ensureAtLeast = kotlin.time.Duration.milliseconds(100)

        val (result, taken) = measureTimedValue {
            val (fanID, fanPassword) =
                client.sql("SELECT fan_id, password FROM fans WHERE email = $1")
                    .bind("$1", email)
                    .map { row ->
                        (row["fan_id"] as? JUUID)?.kornea?.let { fanID ->
                            (row["password"] as? String)?.let { passwd ->
                                Pair(fanID, passwd)
                            }
                        }
                    }.awaitSingleOrNull() ?: return@measureTimedValue null

            if (passwords.matches(password, fanPassword)) {
                fans[fanID]
            } else {
                null
            }
        }

        delay(ensureAtLeast - taken)

        if (result == null) return null

        val authToken = jwt {
            setSubject(email)
            setId(result.id.id)
        }

        return authToken to result
    }
}