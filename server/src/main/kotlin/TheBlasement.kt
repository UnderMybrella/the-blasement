import com.soywiz.klock.DateTime
import com.soywiz.klock.parse
import dev.brella.blasement.B2Api
import dev.brella.blasement.BlasebackMachineAccelerated
import dev.brella.blasement.BlasebackMachineConcurrent
import dev.brella.blasement.BlasementLeague
import dev.brella.blasement.bindAs
import dev.brella.blasement.bindAsNullable
import dev.brella.blasement.blaseback.BlasementDataSource
import dev.brella.blasement.common.*
import dev.brella.blasement.common.events.*
import dev.brella.blasement.utils.get
import dev.brella.blasement.utils.klockHours
import dev.brella.blasement.utils.seconds
import dev.brella.kornea.blaseball.BlaseballApi
import dev.brella.kornea.blaseball.base.common.BLASEBALL_TIME_PATTERN
import dev.brella.kornea.blaseball.base.common.EnumBlaseballSnack
import dev.brella.kornea.blaseball.base.common.ItemID
import dev.brella.kornea.blaseball.base.common.ModificationID
import dev.brella.kornea.blaseball.base.common.PlayerID
import dev.brella.kornea.blaseball.base.common.TeamID
import dev.brella.kornea.blaseball.chronicler.ChroniclerApi
import dev.brella.kornea.errors.common.doOnSuccess
import dev.brella.kornea.errors.common.flatMap
import dev.brella.ktornea.common.getAsResult
import dev.brella.ktornea.common.postAsResult
import dev.brella.ktornea.common.putAsResult
import io.jsonwebtoken.JwtBuilder
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.client.utils.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.websocket.*
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.Option
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.serialization.SerializationException
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
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap
import kotlin.coroutines.CoroutineContext
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue
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

    //    val liveData = LiveData(blaseballApi, chroniclerApi, this)
    val fans: MutableMap<KUUID, BlasementHostFan> = HashMap()

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
    val blasementInitialisationJob = launch {

        try {
            client.sql("CREATE TABLE IF NOT EXISTS fans (fan_id uuid NOT NULL, email VARCHAR(128), apple_id VARCHAR(128), google_id VARCHAR(128), discord_id VARCHAR(128), name VARCHAR(128), password VARCHAR(128), coins BIGINT NOT NULL DEFAULT 250, last_active VARCHAR(128) NOT NULL, created VARCHAR(128) NOT NULL, login_streak INT NOT NULL DEFAULT 0, idol uuid, favourite_team uuid, has_unlocked_shop BOOLEAN NOT NULL DEFAULT FALSE, has_unlocked_elections BOOLEAN NOT NULL DEFAULT FALSE, peanuts_eaten INT NOT NULL DEFAULT 0, squirrels INT NOT NULL DEFAULT 0, light_mode BOOLEAN NOT NULL DEFAULT false, inventory_space INT NOT NULL DEFAULT 8, spread VARCHAR(128) NOT NULL DEFAULT '', coffee INT, fav_number INT, read_only BOOLEAN NOT NULL DEFAULT false, verified BOOLEAN NOT NULL DEFAULT FALSE, active_league_type VARCHAR(128), active_league_id VARCHAR(128));")
                .await()

            client.sql("CREATE TABLE IF NOT EXISTS items (id BIGSERIAL PRIMARY KEY, fan_id uuid NOT NULL, item_name VARCHAR(64) NOT NULL, quantity INT NOT NULL DEFAULT 0);")
                .await()

            client.sql("CREATE TABLE IF NOT EXISTS bets (id BIGSERIAL PRIMARY KEY, fan_id uuid NOT NULL, game_id uuid NOT NULL, team_id uuid NOT NULL, amount INT NOT NULL);")
                .await()

            client.sql("CREATE TABLE IF NOT EXISTS trackers (fan_id uuid NOT NULL, begs INT NOT NULL DEFAULT 0, bets INT NOT NULL DEFAULT 0, votes_cast INT NOT NULL DEFAULT 0, snacks_bought INT NOT NULL DEFAULT 0, snack_upgrades INT NOT NULL DEFAULT 0);")
                .await()

            client.sql("CREATE TABLE IF NOT EXISTS toasts (id BIGSERIAL PRIMARY KEY, fan_id uuid NOT NULL, toast VARCHAR (256) NOT NULL, timestamp BIGINT NOT NULL)")
                .await()

            client.sql("SELECT fan_id, email, apple_id, google_id, discord_id, name, password, coins, last_active, created, login_streak, idol, favourite_team, has_unlocked_shop, has_unlocked_elections, peanuts_eaten, squirrels, light_mode, inventory_space, spread, coffee, fav_number, read_only, verified, active_league_type, active_league_id FROM fans;")
                .map { row ->
                    BlasementFanDatabasePayload(
                        FanID((row["fan_id"] as? JUUID)?.kornea ?: (row["fan_id"] as? String)?.uuidOrNull() ?: throw IllegalArgumentException("No fan_id found for $row")),
                        row["email"] as? String,
                        row["apple_id"] as? String,
                        row["google_id"] as? String,
                        row["discord_id"] as? String,
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
                        row["fav_number"] as? Int,
                        row["read_only"] as Boolean,
                        row["verified"] as Boolean,
                        row["active_league_type"] as? String,
                        row["active_league_id"] as? String
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
//
//    private val gamesToday = ValueCache { date: BlaseballDate -> blaseballApi.getGamesByDate(date.season, date.day).get() }
//    private val gamesTomorrow = ValueCache { date: BlaseballDate -> blaseballApi.getGamesByDate(date.season, date.day + 1).get() }

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

    inline fun parser() =
        Jwts.parserBuilder()
            .requireIssuer("3de20b85-df54-4894-8d23-057796cd1a3b")
            .setSigningKey(privateKey)

    val b2 = configJson?.getJsonObjectOrNull("b2")?.let { b2Json ->
        B2Api(
            b2Permits = b2Json.getIntOrNull("permits"),
            bucketID = b2Json.getString("bucket_id"),
            applicationKeyId = b2Json.getString("application_key_id"),
            applicationKey = b2Json.getString("application_key")
        )
    }

    inline fun jwt(builder: JwtBuilder.() -> Unit) =
        Jwts.builder()
            .setIssuer("3de20b85-df54-4894-8d23-057796cd1a3b")
            .signWith(privateKey)
            .apply(builder)
            .compact()

//    suspend fun today(): BlaseballDate =
//        liveData.date ?: blaseballApi.getSimulationData().get().run { BlaseballDate(season, day) }
//
//    suspend fun gamesToday(): List<BlaseballDatabaseGame> =
//        gamesToday.get(today())
//
//    suspend fun gamesTomorrow(): List<BlaseballDatabaseGame> =
//        gamesTomorrow.get(today())

    suspend fun newFanWithEmail(fanID: JUUID, email: String, password: String, leagueType: String, leagueID: String) = newFanWithEmail(fanID.kornea, email, password, leagueType, leagueID)
    suspend fun newFanWithEmail(fanID: KUUID, email: String, password: String, leagueType: String, leagueID: String): Pair<String, BlasementHostFan> {
        val authToken = jwt {
            setSubject(email)
            setAudience("$leagueType-$leagueID")
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

            loginStreak = 0,

            activeLeagueType = leagueType,
            activeLeagueID = leagueID
        )

        fan.apply {
            setItemQuantity(EnumBlaseballSnack.VOTES) { 1 }
            setItemQuantity(EnumBlaseballSnack.SNAKE_OIL) { 1 }
            setItemQuantity(EnumBlaseballSnack.PEANUTS) { 10 }

            setItemQuantity(EnumBlaseballSnack.FLUTES) { 10 }
        }

        try {
            client.sql("INSERT INTO fans (fan_id, email, apple_id, google_id, discord_id, name, password, last_active, created, coins, idol, favourite_team, has_unlocked_shop, has_unlocked_elections, coffee, fav_number, active_league_type, active_league_id) VALUES ( $1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18 )")
                .bindAs("$1", fanID)
                .bindNullable("$2", fan.email)
                .bindNullable("$3", fan.appleId)
                .bindNullable("$4", fan.googleId)
                .bindNullable("$5", fan.discordId)
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
                .bindNullable<String>("$17", fan.activeLeagueType)
                .bindNullable<String>("$18", fan.activeLeagueID)
                .await()
        } catch (th: Throwable) {
            th.printStackTrace()
            throw th
        }

        fans[fanID] = fan
        return Pair(authToken, fan)
    }

    suspend fun newFanWithDiscord(fanID: JUUID, discordId: String, leagueType: String, leagueID: String) = newFanWithDiscord(fanID.kornea, discordId, leagueType, leagueID)
    suspend fun newFanWithDiscord(fanID: KUUID, discordId: String, leagueType: String, leagueID: String): Pair<String, BlasementHostFan> {
        val authToken = jwt {
            setSubject(discordId)
            setId(fanID.toString())
            setAudience("$leagueType-$leagueID")
        }

        val now = DateTime.now().utc

        val fan = BlasementHostFan(
            blasement = this,
            id = FanID(fanID),
            discordId = discordId,
            coins = 250,
            favouriteTeam = null,

            lastActive = now,
            created = now,

            loginStreak = 0,

            activeLeagueType = leagueType,
            activeLeagueID = leagueID
        )

        fan.apply {
            setItemQuantity(EnumBlaseballSnack.VOTES) { 1 }
            setItemQuantity(EnumBlaseballSnack.SNAKE_OIL) { 1 }
            setItemQuantity(EnumBlaseballSnack.PEANUTS) { 10 }

            setItemQuantity(EnumBlaseballSnack.FLUTES) { 10 }
        }

        try {
            client.sql("INSERT INTO fans (fan_id, email, apple_id, google_id, discord_id, name, password, last_active, created, coins, idol, favourite_team, has_unlocked_shop, has_unlocked_elections, coffee, fav_number, active_league_type, active_league_id) VALUES ( $1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18 )")
                .bindAs("$1", fanID)
                .bindNullable("$2", fan.email)
                .bindNullable("$3", fan.appleId)
                .bindNullable("$4", fan.googleId)
                .bindNullable("$5", fan.discordId)
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
                .bindNullable<String>("$17", fan.activeLeagueType)
                .bindNullable<String>("$18", fan.activeLeagueID)
                .await()
        } catch (th: Throwable) {
            th.printStackTrace()
            throw th
        }

        fans[fanID] = fan
        return Pair(authToken, fan)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun login(email: String, password: String, leagueType: String, leagueID: String): Pair<String, BlasementHostFan>? {
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
            setAudience("$leagueType-$leagueID")
        }

        result.changeLeague(leagueType, leagueID)

        return authToken to result
    }

    data class DiscordOAuthState(val redirectUrl: String?, val leagueType: String, val leagueID: String)

    val discordOAuthState: MutableMap<String, DiscordOAuthState> = ConcurrentHashMap()
    val discordOAuthRandom = Random()
    val discordOAuthStateAlphabet = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM1234567890"

    suspend fun redirectDiscord(call: ApplicationCall, state: DiscordOAuthState) {
        call.respondRedirect(URLBuilder("https://discord.com/api/oauth2/authorize").apply {
            with(parameters) {
                append("client_id", configJson?.getStringOrNull("discord_client_id") ?: return call.respondJsonObject(HttpStatusCode.ServiceUnavailable) {
                    this["error"] = "Discord Login Unavailable"
                })
                append("redirect_uri", "${configJson.getStringOrNull("discord_redirect_url") ?: "https://blasement.brella.dev"}/oauth/discord")
                append("response_type", "code")
                append("scope", "identify")
                append("prompt", "none")

                val code = CharArray(discordOAuthRandom.nextInt(8)) { discordOAuthStateAlphabet[discordOAuthRandom.nextInt(discordOAuthStateAlphabet.length)] }
                    .concatToString()

                discordOAuthState[code] = state
                append("state", code)
            }
        }.buildString())
    }

    fun setupDiscordOAuth(route: Route) =
        route.get("/discord") {
            val parameters = call.request.queryParameters

            val code = parameters["code"] ?: return@get call.respondJsonObject(HttpStatusCode.BadRequest) { this["error"] = "No code provided" }
            val stateCode = parameters["state"] ?: return@get call.respondJsonObject(HttpStatusCode.BadRequest) { this["error"] = "No state provided" }

            httpClient.postAsResult<JsonObject>("https://discord.com/api/oauth2/token") {
                body = FormDataContent(Parameters.build {
                    append("client_id", configJson?.getStringOrNull("discord_client_id") ?: return@get call.respondJsonObject(HttpStatusCode.ServiceUnavailable) {
                        this["error"] = "Discord Login Unavailable"
                    })
                    append("client_secret", configJson.getStringOrNull("discord_client_secret") ?: return@get call.respondJsonObject(HttpStatusCode.ServiceUnavailable) {
                        this["error"] = "Discord Login Unavailable"
                    })
                    append("grant_type", "authorization_code")
                    append("code", code)
                    append("redirect_uri", "${configJson.getStringOrNull("discord_redirect_url") ?: "https://blasement.brella.dev"}/oauth/discord")
                })
            }.flatMap { tokenJson ->
                httpClient.getAsResult<JsonObject>("https://discord.com/api/users/@me") {
                    header("Authorization", "Bearer ${tokenJson.getStringOrNull("access_token")}")
                }
            }.doOnSuccess { discordUser ->
                val discordId = discordUser.getString("id")
                val state = discordOAuthState.remove(stateCode) ?: return@get call.respondJsonObject(HttpStatusCode.BadRequest) { this["error"] = "No state for code $stateCode" }

                val fanID =
                    client.sql("SELECT fan_id FROM fans WHERE discord_id = $1")
                        .bind("$1", discordId)
                        .map { row -> (row["fan_id"] as? JUUID)?.kornea }
                        .awaitSingleOrNull()

                if (fanID != null) {
                    fans[fanID]?.changeLeague(state.leagueType, state.leagueID)

                    call.response.cookies.append(COOKIE_NAME, jwt {
                        setSubject(discordId)
                        setAudience("${state.leagueType}-${state.leagueID}")
                        setId(fanID.toString())
                    }, httpOnly = true, path = "/")

                    call.respondRedirect(state.redirectUrl ?: "/discord_landing", permanent = false)
                } else {
                    val (auth) = newFanWithDiscord(JUUID.randomUUID(), discordId, state.leagueType, state.leagueID)

                    call.response.cookies.append(COOKIE_NAME, auth, httpOnly = true, path = "/")

                    call.respondRedirect(state.redirectUrl ?: "/discord_landing", permanent = false)
                }
            }.respondOnFailure(call)
        }


    /** Routing */
    val currentLeagues: MutableMap<String, MutableMap<String, BlasementLeague>> = ConcurrentHashMap()

    inline val ApplicationCall.league
        get() = parameters["league_type"]?.let { type ->
            parameters["league_id"]?.let { id ->
                currentLeagues[type][id]
            }
        }

    fun routingWebpage(route: Route) =
        with(route) {
            handle { call.league?.siteData?.respondIndexHtml(call) }
            get { call.league?.siteData?.respondIndexHtml(call) }
            get("/") { call.league?.siteData?.respondIndexHtml(call) }

            get("/{...}") {
                println("Tailcard url ${call.request.path()}")

                call.league?.siteData?.respondIndexHtml(call)
            }

            get("/main.js") { call.league?.siteData?.respondMainJs(call) }

            get("/2.js") { call.league?.siteData?.respond2Js(call) }

            get("/main.css") { call.league?.siteData?.respondMainCss(call) }
        }

    fun routing(route: Route) =
        with(route) {
            route("/blaseball") {
                route("/{league_type}") {
                    route("/{league_id}") {
                        routingWebpage(this)

                        route("/api") { api() }
                        route("/database") { database() }
                        route("/events") { events() }
                        route("/auth") { auth() }
                    }
                }
            }

            route("/oauth") {
                setupDiscordOAuth(this)
            }
        }

    val authScope = CoroutineScope(SupervisorJob())
    val authRequests: MutableMap<String, Int> = ConcurrentHashMap()

    inline val ApplicationCall.authToken: String?
        get() = request.header("Authorization") ?: request.cookies[TheBlasement.COOKIE_NAME]

    inline fun authValid(key: String): Boolean {
        if ((authRequests[key] ?: 0) >= 3) return true

        authRequests.compute(key) { _, v -> v?.plus(1) ?: 1 }

        authScope.launch {
            delay(10_000)
            authRequests.compute(key) { _, v -> v?.minus(1) }
        }

        return false
    }

    fun Route.auth() {
        post("/local") {
            if (authValid(call.request.origin.remoteHost)) return@post call.respond(HttpStatusCode.TooManyRequests, EmptyContent)

            val payload = call.receive<BlaseballAuthPayload>()
            if (authValid(payload.username)) return@post call.respond(HttpStatusCode.TooManyRequests, EmptyContent)

            if (payload.isLogin) {
                val result = login(payload.username, payload.password, call.parameters.getOrFail("league_type"), call.parameters.getOrFail("league_id"))

                if (result == null) {
                    call.respondJsonObject(HttpStatusCode.BadRequest) { this["error"] = "Error: Wrong password for user" }
                } else {
                    call.response.cookies.append(TheBlasement.COOKIE_NAME, result.first, httpOnly = true, path = "/")

                    call.respondRedirect(call.request.path().substringBeforeLast("/auth/local") + "/", permanent = false)
                }
            } else {
                when {
                    payload.password != payload.passwordConfirm ->
                        call.respondJsonObject(HttpStatusCode.BadRequest) { this["error"] = "Passwords do not match" }
                    fans.any { (_, fan) -> fan.email == payload.username } ->
                        call.respondJsonObject(HttpStatusCode.BadRequest) { this["error"] = "Email already registered" }
                    else -> {
                        val (auth) = newFanWithEmail(
                            fanID = JUUID.randomUUID(),
                            email = payload.username,
                            password = passwords.encode(payload.password),
                            leagueType = call.parameters["league_type"] ?: return@post,
                            leagueID = call.parameters["league_id"] ?: return@post
                        )

                        call.response.cookies.append(TheBlasement.COOKIE_NAME, auth, httpOnly = true, path = "/")

                        call.respondRedirect(call.request.path().substringBeforeLast("/auth/local") + "/", permanent = false)
                    }
                }
            }
        }

        get("/logout") {
            call.response.cookies.appendExpired(TheBlasement.COOKIE_NAME, path = "/")

            call.respondRedirect(call.request.path().substringBeforeLast("/auth/logout") + "/", permanent = false)
        }

        get("/discord") {
            redirectDiscord(
                call,
                DiscordOAuthState(
                    call.request.queryParameters["redirectUrl"] ?: this@auth.toString().substringBeforeLast('/') + "/",
                    call.parameters["league_type"] ?: return@get,
                    call.parameters["league_id"] ?: return@get
                )
            )
        }
    }

    fun Route.api() {
        route("/time") {
            get {
                call.league?.let { league ->
                    call.respondText(BLASEBALL_TIME_PATTERN.format(league.source.now()))
                }
            }
            webSocket {
                call.league?.let { league ->
                    try {
                        while (isActive) {
                            delay(1_000)
                            send(BLASEBALL_TIME_PATTERN.format(league.source.now()))
                        }
                    } catch (th: Throwable) {
//                        th.printStackTrace()
                    }
                }
            }
            get("/sse") {
                call.league?.let { league ->
                    try {
                        call.respondTextWriter(ContentType.Text.EventStream) {
                            while (isActive) {
                                delay(1_000)

                                try {
                                    write("data:")
                                    write(BLASEBALL_TIME_PATTERN.format(league.source.now()))
                                    write("\n\n")
                                    flush()
                                } catch (th: Throwable) {
//                            th.printStackTrace()
                                    close()
                                }
                            }
                        }
                    } catch (th: Throwable) {
//                th.printStackTrace()
                    }
                }
            }
        }

        get("/getUser") {
//            call.respond(call.fan().toFrontendPayload())
            val league = call.league ?: return@get
            val fan = league fanFor call
                      ?: return@get call.respondJsonObject(HttpStatusCode.Unauthorized) {
                          this["error"] = "Invalid auth token."
                      }

            call.respond(fan.toFrontendPayload())
        }
        get("/getUserRewards") {
            val league = call.league ?: return@get
            val fan = league fanFor call
                      ?: return@get call.respondJsonObject(HttpStatusCode.Unauthorized) {
                          this["error"] = "Invalid auth token."
                      }

            call.respondJsonObject {
                this["coins"] = fan.coins
                this["toasts"] = JsonArray(fan.getToasts(league.source.now().utc.unixMillisLong).map(::JsonPrimitive))
                this["lightMode"] = fan.lightMode
            }
        }
        get("/getActiveBets") {
            val league = call.league ?: return@get
            val fan = league fanFor call
                      ?: return@get call.respondJsonObject(HttpStatusCode.Unauthorized) {
                          this["error"] = "Unauthorized"
                      }

            call.respond(fan.currentBets.map { (gameID, bet) ->
                buildJsonObject {
                    this["targets"] = buildJsonArray {
                        add(bet.team.id)
                        add(gameID.id)
                    }

                    this["amount"] = bet.bet
//                    this["type"] = 0
//                    this["userId"] = fan.id.id
                }
            })
        }
        post("/bet") {
            val league = call.league ?: return@post
            val fan = league fanFor call
                      ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                          this["error"] = "Unauthorized"
                      }

            if (fan.readOnly) return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                this["error"] = "Read Only!"
            }

            val bet = call.receive<BlaseballBetPayload>()

//            println("Betting: $bet")

            when (fan.placeBet(onGame = bet.bettingGame, onTeam = bet.bettingTeam, amount = bet.amount)) {
                EnumBetFail.NOT_ENOUGH_COINS -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Not Enough Coins"
                }
                EnumBetFail.BET_TOO_HIGH -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Bet Too High"
                }
                EnumBetFail.CANT_BET_ZERO -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Can't Bet Zero Coins"
                }
                EnumBetFail.INVALID_TEAM -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Invalid Team"
                }
                EnumBetFail.NO_SNAKE_OIL -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "No Snake Oil"
                }
                null -> call.respondJsonObject(HttpStatusCode.OK) {
                    this["message"] = "Bet Placed"
                }
            }
        }

        post("/chooseIdol") {
            val league = call.league ?: return@post
            val fan = league fanFor call
                      ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                          this["error"] = "Invalid auth token."
                      }

            if (fan.readOnly) return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                this["error"] = "Read Only!"
            }

            val idol = call.receive<BlaseballChooseIdolPayload>()

            //Check if games are running
            fan.setIdol { idol.playerId }

            call.respond(HttpStatusCode.OK, EmptyContent)
        }
        post("/setFavoriteTeam") {
            val league = call.league ?: return@post
            val fan = league fanFor call
                      ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                          this["error"] = "Invalid auth token."
                      }

            if (fan.readOnly) return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                this["error"] = "Read Only!"
            }

            if (fan.favouriteTeam != null)
                return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Team already set (?)"
                }

            val team = call.receive<BlaseballSetFavouriteTeamPayload>()

            fan.setFavouriteTeam { team.teamId }

            call.respond(HttpStatusCode.NoContent, EmptyContent)
        }
        post("/updateFavoriteTeam") {
            val league = call.league ?: return@post
            val fan = league fanFor call
                      ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                          this["error"] = "Invalid auth token."
                      }

            if (fan.readOnly) return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                this["error"] = "Read Only!"
            }

            val team = call.receive<BlaseballUpdateFavouriteTeamPayload>()

            when (fan.changeTeam(team.newTeamId).second) {
                EnumChangeTeamFail.NO_FLUTE -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "No Flutes in your inventory."
                }
                EnumChangeTeamFail.ALREADY_FAVOURITE_TEAM -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "${team.teamName} is already your favourite team."
                }
                EnumChangeTeamFail.GAME_IN_PROGRESS -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Cannot change team while game is in progress"
                }
                null -> call.respond(HttpStatusCode.NoContent, EmptyContent)
            }
        }

        post("/upNut") {
            val league = call.league ?: return@post
            val fan = league fanFor call
                      ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                          this["error"] = "Unauthorized"
                      }

            if (fan.readOnly) return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                this["error"] = "Read Only!"
            }

            val payload = call.receive<BlaseballUpNutPayload>()

            val existingPeanuts = fan.inventory[EnumBlaseballSnack.PEANUTS] ?: 0

            if (existingPeanuts <= 0) return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                this["error"] = "You don't have any Peanuts."
            }

            httpClient.putAsResult<String>("$UPNUTS_HOST/${payload.eventId.id}/3de20b85-df54-4894-8d23-057796cd1a3b") {
                call.authToken?.let { header("Authorization", it) }

                parameter("source", fan.id.id)
                parameter("time", BLASEBALL_TIME_PATTERN.format(league.source.now()))
            }.doOnSuccess {
                fan.removeItemQuantity(EnumBlaseballSnack.PEANUTS) { 1 }

                call.respondJsonObject {
                    this["message"] = "You Upshelled an event."
                }
            }.respondOnFailure(call)
        }
        post("/eatADangPeanut") {
            val league = call.league ?: return@post
            val fan = league fanFor call
                      ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                          this["error"] = "Unauthorized"
                      }

            if (fan.readOnly) return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                this["error"] = "Read Only!"
            }

            val payload = call.receive<BlaseballEatADangNutPayload>()

            val existingPeanuts = fan.inventory[EnumBlaseballSnack.PEANUTS] ?: 0
            val eaten = payload.amount

            if (existingPeanuts < eaten) return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                this["error"] = "You don't have enough Peanuts."
            } else if (eaten <= 0) return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                this["error"] = "Invalid Peanut count."
            }

//            fans[fanID] = fan.copy(peanutsEaten = fan.peanutsEaten + eaten, snacks = fan.snacks.copy(peanuts = existingPeanuts - eaten))
//            fan.peanutsEaten
            fan.removeItemQuantity(EnumBlaseballSnack.PEANUTS) { eaten }

            call.respond(HttpStatusCode.OK, EmptyContent)
        }

        post("/buyUnlockShop") {
            val league = call.league ?: return@post
            val fan = league fanFor call
                      ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                          this["error"] = "Unauthorized"
                      }

            if (fan.readOnly) return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                this["error"] = "Read Only!"
            }

            //*Technically*, this responds with a 200 every time - however, it also takes 20 coins from the player even if they have the shop unlocked
            //TODO: Compat mode?
            //NOTE: Possible race condition?
            val (_, error) = fan.purchaseShopMembershipCard()

            when (error) {
                null -> call.respondJsonObject {
                    this["message"] = "Shop Unlocked"
                }

                EnumUnlockFail.NOT_ENOUGH_COINS -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Not Enough Coins"
                }
                EnumUnlockFail.ALREADY_UNLOCKED -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Shop Already Unlocked"
                }
            }
        }
        post("/buyUnlockElection") {
            val league = call.league ?: return@post
            val fan = league fanFor call
                      ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                          this["error"] = "Unauthorized"
                      }

            if (fan.readOnly) return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                this["error"] = "Read Only!"
            }

            //*Technically*, this responds with a 200 every time - however, it also takes 20 coins from the player even if they have the shop unlocked
            //TODO: Compat mode?
            //NOTE: Possible race condition?
            val (_, error) = fan.purchaseVotingRights()

            when (error) {
                null -> call.respondJsonObject {
                    this["message"] = "Voting Rights Unlocked"
                }

                EnumUnlockFail.NOT_ENOUGH_COINS -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Not Enough Coins"
                }
                EnumUnlockFail.ALREADY_UNLOCKED -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Voting Rights Already Unlocked"
                }
            }
        }

        post("/updateProfile") {
            val league = call.league ?: return@post
            val fan = league fanFor call
                      ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                          this["error"] = "Unauthorized"
                      }

            if (fan.readOnly) return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                this["error"] = "Read Only!"
            }

            val payload = call.receive<BlaseballUpdateProfilePayload>()

            fan.setFavNumber { payload.favNumber }
            fan.setCoffee { payload.coffee }

            call.respond(HttpStatusCode.OK, EmptyContent)
        }
        post("/updateSettings") {
            val league = call.league ?: return@post
            val fan = league fanFor call
                      ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                          this["error"] = "Unauthorized"
                      }

            if (fan.readOnly) return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                this["error"] = "Read Only!"
            }

            call.respond(HttpStatusCode.OK, EmptyContent)
        }

        redirectInternally("/buySnackNoUpgrade", "/buySnack")
        post("/buySnack") {
            val league = call.league ?: return@post
            val fan = league fanFor call
                      ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                          this["error"] = "Unauthorized"
                      }

            if (fan.readOnly) return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                this["error"] = "Read Only!"
            }

            val payload = call.receive<BlaseballBuySnackPayload>()

            val item = EnumBlaseballSnack.fromID(payload.snackId)
                       ?: return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                           this["error"] = "Unknown Snack"
                       }

            val (_, error) = fan.buySnack(1, item)

            when (error) {
                EnumPurchaseItemFail.MEMBERSHIP_LOCKED -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "No Shop Membership"
                }
                EnumPurchaseItemFail.NOT_ENOUGH_COINS -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Not Enough Coins"
                }
                EnumPurchaseItemFail.TIER_TOO_HIGH -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Tier Too High"
                }
                EnumPurchaseItemFail.INVENTORY_FULL -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Inventory Full"
                }
                EnumPurchaseItemFail.ITEM_COUNT_FULL -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Item Count Full"
                }
                null -> call.respondJsonObject(HttpStatusCode.OK) {
                    this["message"] = "You Bought 1 ${item.name.split('_').joinToString(" ") { it.toLowerCase().capitalize() }}"
                }
            }
        }
        post("/sellSnack") {
            val league = call.league ?: return@post
            val fan = league fanFor call
                      ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                          this["error"] = "Unauthorized"
                      }

            if (fan.readOnly) return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                this["error"] = "Read Only!"
            }

            val payload = call.receive<BlaseballSellSnackPayload>()

            val item = EnumBlaseballSnack.fromID(payload.snackId)
                       ?: return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                           this["error"] = "Unknown Snack"
                       }

            val (profit, error) = fan.sell(payload.amount, item)

            when (error) {
                EnumSellItemFail.MEMBERSHIP_LOCKED -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "No Shop Membership"
                }
                EnumSellItemFail.ITEM_NOT_IN_INVENTORY -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Item Not in Inventory"
                }
                EnumSellItemFail.NOT_ENOUGH_ITEMS -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Not Enough Items"
                }
                null -> call.respondJsonObject(HttpStatusCode.OK) {
                    this["message"] = "You Sold ${payload.amount} ${item.name.split('_').joinToString(" ") { it.toLowerCase().capitalize() }}"
                }
            }
        }

        post("/buySlot") {
            val league = call.league ?: return@post
            val fan = league fanFor call
                      ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                          this["error"] = "Unauthorized"
                      }

            if (fan.readOnly) return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                this["error"] = "Read Only!"
            }

            val (_, error) = fan.purchaseSlot()

            when (error) {
                EnumPurchaseSlotFail.MEMBERSHIP_LOCKED -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "No Shop Membership"
                }
                EnumPurchaseSlotFail.NOT_ENOUGH_COINS -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Not Enough Coins"
                }
                EnumPurchaseSlotFail.TOO_MANY_SLOTS -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Too Many Slots"
                }
                EnumPurchaseSlotFail.INVALID_AMOUNT -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Invalid Amount To Buy"
                }

                null -> call.respondJsonObject(HttpStatusCode.OK) {
                    this["message"] = "You bought a snack slot."
                }
            }

            call.respond(HttpStatusCode.OK, EmptyContent)
        }
        post("/sellSlot") {
            val league = call.league ?: return@post
            val fan = league fanFor call
                      ?: return@post call.respondJsonObject(HttpStatusCode.Unauthorized) {
                          this["error"] = "Unauthorized"
                      }

            if (fan.readOnly) return@post call.respondJsonObject(HttpStatusCode.BadRequest) {
                this["error"] = "Read Only!"
            }

            val payload = call.receive<BlaseballSellSlotPayload>()

            //We don't really care about indices atm
            val (_, error) = fan.sellSlot()

            when (error) {
                EnumSellSlotFail.MEMBERSHIP_LOCKED -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "No Shop Membership"
                }
                EnumSellSlotFail.NO_EMPTY_SLOTS -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "No Empty Slots"
                }
                EnumSellSlotFail.NOT_ENOUGH_SLOTS -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Not Enough Slots"
                }
                EnumSellSlotFail.INVALID_AMOUNT -> call.respondJsonObject(HttpStatusCode.BadRequest) {
                    this["error"] = "Invalid Amount to Sell"
                }

                null -> call.respondJsonObject(HttpStatusCode.OK) {
                    this["message"] = "You bought a snack slot."
                }
            }
        }

        redirectInternally("/idol_board", "/getIdols")
        get("/getIdols") {
            call.league?.source?.getIdolBoard()?.respond(call)
        }

        redirectInternally("/hall_of_flame", "/getTribute")
        get("/getTribute") {
            call.league?.source?.getHallOfFlamePlayers()?.respond(call)
        }
    }

    fun Route.database() {
        get("/blood") {
            call.league?.source?.getBloodTypes(
                call.parameters.getAll("ids")
                ?: call.parameters["id"]?.let(::listOf)
                ?: emptyList()
            )?.respond(call)
        }

        get("/coffee") {
            call.league?.source?.getCoffeePreferences(
                call.parameters.getAll("ids")
                ?: call.parameters["id"]?.let(::listOf)
                ?: emptyList()
            )?.respond(call)
        }

        get("/items") {
            call.league?.source?.getItems(
                call.parameters.getAll("ids")?.map(::ItemID)
                ?: call.parameters["id"]?.let(::listOf)?.map(::ItemID)
                ?: emptyList()
            )?.respond(call)
        }

        get("/mods") {
            call.league?.source?.getModifications(
                call.parameters.getAll("ids")?.map(::ModificationID)
                ?: call.parameters["id"]?.let(::listOf)?.map(::ModificationID)
                ?: emptyList()
            )?.respond(call)
        }

        get("/players") {
            call.league?.source?.getPlayers(
                call.parameters.getAll("ids")?.flatMap { it.split(',') }?.map(::PlayerID)
                ?: call.parameters["id"]?.let(::listOf)?.flatMap { it.split(',') }?.map(::PlayerID)
                ?: emptyList()
            )?.respond(call)
        }

        redirectInternally("/ticker", "/globalEvents")
        get("/globalEvents") { call.league?.source?.getGlobalEvents()?.respond(call) }

        redirectInternally("/sim", "/simulationData")
        get("/simulationData") { call.league?.source?.getSimulationData()?.respond(call) }

        redirectInternally("/feed/phase", "/feedbyphase")
        get("/feedbyphase") {
            try {
                call.league?.source?.getFeedByPhase(
                    phase = call.parameters.getOrFail("phase").toInt(),
                    season = call.parameters.getOrFail("season").toInt()
                )?.respond(call)
            } catch (th: Throwable) {
                th.printStackTrace()
            }
        }

        route("/feed") {
            get("/global") {
                try {
                    val league = call.league ?: return@get
                    val fan = league fanFor call

                    league.source.getGlobalFeed(
                        category = call.parameters["category"]?.toIntOrNull(),
                        limit = call.parameters["limit"]?.toIntOrNull() ?: 100,
                        type = call.parameters["type"]?.toIntOrNull(),
                        sort = call.parameters["sort"]?.toIntOrNull(),
                        start = call.parameters["start"],
                        fanID = fan?.id
                    ).respond(call)
                } catch (th: Throwable) {
                    th.printStackTrace()
                }
            }

            get("/player") {
                try {
                    val league = call.league ?: return@get
                    val fan = league fanFor call

                    league.source.getPlayerFeed(
                        id = PlayerID(call.parameters.getOrFail("id")),
                        category = call.parameters["category"]?.toIntOrNull(),
                        limit = call.parameters["limit"]?.toIntOrNull() ?: 100,
                        type = call.parameters["type"]?.toIntOrNull(),
                        sort = call.parameters["sort"]?.toIntOrNull(),
                        start = call.parameters["start"],
                        fanID = fan?.id
                    ).respond(call)
                } catch (th: Throwable) {
                    th.printStackTrace()
                }
            }

            get("/team") {
                try {
                    val parameters = call.request.queryParameters

                    val league = call.league ?: return@get
                    val fan = league fanFor call

                    league.source.getTeamFeed(
                        id = TeamID(parameters.getOrFail("id")),
                        category = parameters["category"]?.toIntOrNull(),
                        limit = parameters["limit"]?.toIntOrNull() ?: 100,
                        type = parameters["type"]?.toIntOrNull(),
                        sort = parameters["sort"]?.toIntOrNull(),
                        start = parameters["start"],
                        fanID = fan?.id
                    ).respond(call)
                } catch (th: Throwable) {
                    th.printStackTrace()
                }
            }
        }

        get("/offseasonSetup") {
            try {
                val response = httpClient.get<HttpResponse>("https://www.blaseball.com/database/offseasonSetup?${call.request.uri.substringAfter('?')}")
                call.respondBytes(response.receive(), response.contentType())
            } catch (th: Throwable) {
                th.printStackTrace()
            }
        }
    }

    fun Route.events() {
        get("/streamData") {
            call.league?.let { league ->
                try {
                    call.respondTextWriter(ContentType.Text.EventStream) {
                        league.source.getLiveDataStream()
                            .doOnSuccess { flow ->
                                flow.collect { data ->
                                    try {
                                        write("data:")
                                        write(data)
                                        write("\n\n")
                                        flush()
                                    } catch (th: Throwable) {
//                                    th.printStackTrace()
                                        close()
                                    }
                                }
                            }
                    }
                } catch (th: Throwable) {
//                th.printStackTrace()
                }
            }
        }
    }

    fun registerLeague(type: String, id: String, source: BlasementDataSource): BlasementLeague {
        val league = BlasementLeague(this, source, type, id, "/blaseball/$type/$id")
        currentLeagues.computeIfAbsent(type) { ConcurrentHashMap() }[id] = league
        return league
    }

    init {
        registerLeague("hyperdrive", "mass_production", BlasebackMachineAccelerated.massProduction(httpClient, blaseballApi, json, 60.seconds, 15.klockHours))
        registerLeague("hyperdrive", "collections", BlasebackMachineAccelerated.collections(httpClient, blaseballApi, json, 60.seconds, 15.klockHours))
        registerLeague("hyperdrive", "live_bait", BlasebackMachineAccelerated.liveBait(httpClient, blaseballApi, json, 60.seconds, 15.klockHours))

        registerLeague("accelerated", "mass_production", BlasebackMachineAccelerated.massProduction(httpClient, blaseballApi, json, 5.seconds, 15.klockHours))
        registerLeague("accelerated", "collections", BlasebackMachineAccelerated.collections(httpClient, blaseballApi, json, 5.seconds, 15.klockHours))
        registerLeague("accelerated", "live_bait", BlasebackMachineAccelerated.liveBait(httpClient, blaseballApi, json, 5.seconds, 15.klockHours))

        registerLeague("concurrent", "mass_production", BlasebackMachineConcurrent.massProduction(httpClient, blaseballApi, json))
        registerLeague("concurrent", "collections", BlasebackMachineConcurrent.collections(httpClient, blaseballApi, json))
        registerLeague("concurrent", "live_bait", BlasebackMachineConcurrent.liveBait(httpClient, blaseballApi, json))
    }
}