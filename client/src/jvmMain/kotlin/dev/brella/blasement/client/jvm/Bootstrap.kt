package dev.brella.blasement.client.jvm

import dev.brella.blasement.common.events.BlasementFan
import dev.brella.blasement.common.events.ClientEvent
import dev.brella.blasement.common.events.EnumBegFail
import dev.brella.kornea.blaseball.BlaseballApi
import dev.brella.kornea.blaseball.base.common.EnumBlaseballItem
import dev.brella.kornea.blaseball.chronicler.ChroniclerApi
import dev.brella.kornea.errors.common.getOrNull
import dev.brella.ktornea.common.installGranularHttp
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.features.compression.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.StringFormat
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.time.ExperimentalTime

suspend fun WebSocketSession.sendEvent(format: SerialFormat, event: ClientEvent) {
    when (format) {
        is StringFormat -> send(format.encodeToString(event))
        is BinaryFormat -> send(format.encodeToByteArray(event))
        else -> throw IllegalArgumentException("Unknown format type ${format::class} (${format::class.java.interfaces.joinToString()}")
    }
}

@OptIn(ExperimentalTime::class)
suspend fun main() {
    val json = Json { }

    val client = HttpClient(OkHttp) {
        installGranularHttp()

        install(ContentEncoding) {
            gzip()
            deflate()
            identity()
        }

        install(JsonFeature) {
            serializer = KotlinxSerializer(json)
        }

        WebSockets {
            pingInterval = 60_000L
        }

        expectSuccess = false

        defaultRequest {
            userAgent("Mozilla/5.0 (X11; Linux x86_64; rv:85.0) Gecko/20100101 Firefox/85.0")
        }
    }

    val blaseballApi = BlaseballApi(client)
    val chroniclerApi = ChroniclerApi(client)

    client.webSocket("ws://localhost:9897/connect") {
        try {
            val clientele = BlasementClientele(json, client, blaseballApi, chroniclerApi, this)
//            clientele.createNewUser("Zanna Testingbench", TeamID("46358869-dce9-4a01-bfba-ac24fc56f57e"))
            if (clientele.authenticate("eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJaYW5uYSBUZXN0aW5nYmVuY2giLCJqdGkiOiI0Y2Q4NTRmOC01NDg2LTRhY2MtODViMS1kZmZhOGJjN2FjZjYifQ.LlQT8xShgU5zxssWK9TjXf-YRIpwhCHMHxAT-OMtfk38o6AE7dc5RqAQ0gdLhu4bwbmouIVqm7q84yPvDuqCNg") != null) {
                println("Unauthenticated :(")
            }

            val fan = clientele.fan

            suspend fun BlasementFan.begZanna() {
                if (coins == 0L) {
                    val (gotCoins, failReason) = beg()
                    if (gotCoins != null) {
                        println("Oh !! I managed to find $gotCoins coins stashed behind the couch, thank god !!")
                    } else if (failReason == EnumBegFail.NO_BREAD_CRUMBS) {
                        println("Laying out a sacrifice for the old gods...")

                        val sacrifice = purchase(1, EnumBlaseballItem.BREAD_CRUMBS)
                        if (sacrifice == null) {
                            println("The ritual is complete.")
                        } else {
                            println("'''$sacrifice'''... goddamn lawyers :(")
                        }
                    } else {
                        println("Oh... The Blaseball Gods have cursed me upon this day, saying that $failReason")
                    }
                }
            }

            if (fan != null) {
                println("Hi there! My name is ${fan.name}")

                var lastPlay = 0

                while (isActive) {
                    val simData = blaseballApi.getSimulationData()
                        .getOrNull()

                    if (simData == null) {
                        delay(2500)
                        continue
                    }

                    if (simData.day == lastPlay) {
                        delay(60_000)
                        continue
                    }

                    println("Starting the day off with ${fan.coins} coins!")

                    lastPlay = simData.day
                    val scheduled = blaseballApi.getGamesByDate(season = simData.season, day = simData.day + 1, tournament = simData.tournament)
                        .getOrNull()

                    if (scheduled == null) {
                        delay(2500)
                        continue
                    }

                    if (!fan.hasUnlockedShop) {
                        val membershipCardPurchase = fan.purchaseShopMembershipCard()

                        if (membershipCardPurchase == null) {
                            println("Time to be a part of the shop !! Wait, wait no, oh god --")
                        } else {
                            println("Well, turns out this shop doesn't sell $membershipCardPurchase")
                        }
                    }

                    val maxBet = fan.inventory[EnumBlaseballItem.SNAKE_OIL]?.minus(1)?.let(EnumBlaseballItem.SNAKE_OIL::get)?.payout ?: 0
                    if (maxBet == 0) {
                        println("Oh, it looks like my max bet is 0 coins... that's gonna be a problem, right?")

                        delay(60_000)
                        continue
                    }

                    val coinsNeededToBeEffective = fan.inventory[EnumBlaseballItem.SNAKE_OIL]?.let(EnumBlaseballItem.SNAKE_OIL::get)?.price?.plus((maxBet * 1.1) * (scheduled.size * 1.1))
                    if (coinsNeededToBeEffective != null && fan.coins > coinsNeededToBeEffective) {
                        if (fan.hasUnlockedShop) {
                            println("Aha, time to buy some more snake oil, since we have $coinsNeededToBeEffective")

                            val (cost, purchaseResult) = fan.purchase(1, EnumBlaseballItem.SNAKE_OIL)

                            if (purchaseResult == null) {
                                println("We did it, we upgraded our betting power !!")
                            } else {
                                println("I went in and my wallet just turned into $purchaseResult; that's not normal right?")
                            }
                        }
                    }

                    scheduled.forEach { game ->
                        if (game.id in fan.currentBets) return@forEach

                        //Zanna is a safe better and so will bet on games with >57% odds
                        fan.begZanna()

//                        if (game.homeOdds > 0.57) {
//                            val betting = fan.coins.toInt().coerceAtMost(maxBet)
//                            if (betting > 0) {
//                                val betFail = fan.placeBet(game.id, game.homeTeam, fan.coins.toInt().coerceAtMost(maxBet))
//                                if (betFail == null) {
//                                    println("Just placed a bet of $betting coins on ${game.homeTeamName}... Good luck ${game.homeTeamEmoji.removePrefix("0x")} !")
//                                } else {
//                                    println("They threw me out of the betting office... something about $betFail...")
//                                }
//                            }
//                        } else if (game.awayOdds > 0.57) {
                        val betting = fan.coins.toInt().coerceAtMost(maxBet)
                        if (betting > 0) {
                            val betFail = fan.placeBet(game.id, if (Random.nextDouble() <= game.homeOdds) game.homeTeam else game.awayTeam, betting)
                            if (betFail == null) {
                                println("Just placed a bet of $betting coins on ${game.awayTeamName}... Good luck ${game.awayTeamEmoji.removePrefix("0x")} !")
                            } else {
                                println("They threw me out of the betting office... something about $betFail...")
                            }
                        }
//                        }
                    }

                    delay(60_000)
                }
            }

            clientele.join()
        } catch (th: Throwable) {
            th.printStackTrace()
            throw th
        }
    }
}