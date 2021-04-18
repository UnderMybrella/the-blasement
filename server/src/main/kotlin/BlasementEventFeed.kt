import dev.brella.blasement.common.events.*
import dev.brella.kornea.blaseball.BlaseballFeedEventType
import dev.brella.kornea.blaseball.beans.BlaseballFeedEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.CoroutineContext

class BlasementEventFeed(val feed: BlaseballFeed, val liveData: LiveData, scope: CoroutineScope, context: CoroutineContext) {
    private val _onGameEnd: MutableSharedFlow<BlaseballGameEndEvent> = MutableSharedFlow()
    private val _onShutout: MutableSharedFlow<BlaseballShutoutEvent> = MutableSharedFlow()
    private val _onBlackHole: MutableSharedFlow<BlaseballBlackHoleEvent> = MutableSharedFlow()
    private val _onIncineration: MutableSharedFlow<BlaseballIncinerationEvent> = MutableSharedFlow()
    private val _onStrikeout: MutableSharedFlow<BlaseballStrikeoutEvent> = MutableSharedFlow()
    private val _onTeamShamed: MutableSharedFlow<BlaseballTeamShamedEvent> = MutableSharedFlow()
    private val _onTeamShames: MutableSharedFlow<BlaseballTeamShamesEvent> = MutableSharedFlow()
    private val _onHomeRun: MutableSharedFlow<BlaseballHomeRunEvent> = MutableSharedFlow()
    private val _onHit: MutableSharedFlow<BlaseballHitEvent> = MutableSharedFlow()
    private val _onStolenBase: MutableSharedFlow<BlaseballStolenBaseEvent> = MutableSharedFlow()
    private val _onSun2: MutableSharedFlow<BlaseballSun2Event> = MutableSharedFlow()
    private val _onFlood: MutableSharedFlow<BlaseballFloodingEvent> = MutableSharedFlow()

    private val collector = scope.launch(context) {
        feed.flow.onEach { event ->
            val (event, gameStep) = event
            when (event) {
                is BlaseballFeedEvent.GameEndLog -> if (gameStep != null) {
                    _onGameEnd.emit(BlaseballGameEndEvent(event, gameStep, event.metadata.winner))

                    //TODO: Triple check if this is valid shutout logic
                    if (gameStep.homePitcher != null && gameStep.awayScore == 0.0) {
                        _onShutout.emit(BlaseballShutoutEvent(event, gameStep, gameStep.homePitcher!!, gameStep.homePitcherName!!))
                    } else if (gameStep.awayPitcher != null && gameStep.homeScore == 0.0) {
                        _onShutout.emit(BlaseballShutoutEvent(event, gameStep, gameStep.awayPitcher!!, gameStep.awayPitcherName!!))
                    }
                }
                is BlaseballFeedEvent.BlackHoleInGame -> if (gameStep != null) _onBlackHole.emit(BlaseballBlackHoleEvent(event, gameStep))
                //Incineration technically has two events, so we should listen to the parent one - that has more information
                is BlaseballFeedEvent.Incineration -> if (gameStep != null && event.metadata.children != null) _onIncineration.emit(BlaseballIncinerationEvent(event, gameStep))
                is BlaseballFeedEvent.Strikeout -> if (gameStep != null) _onStrikeout.emit(BlaseballStrikeoutEvent(event, gameStep))
                is BlaseballFeedEvent.TeamShamed -> _onTeamShamed.emit(BlaseballTeamShamedEvent(event, event.teamTags.first()))
                is BlaseballFeedEvent.TeamShames -> _onTeamShames.emit(BlaseballTeamShamesEvent(event, event.teamTags.first()))
                is BlaseballFeedEvent.HomeRun -> if (gameStep != null) _onHomeRun.emit(BlaseballHomeRunEvent(event, gameStep))
                is BlaseballFeedEvent.Hit -> {
                    if (gameStep != null) {
                        if (gameStep.homePitcher != null) _onHit.emit(BlaseballHitEvent(event, gameStep, gameStep.homePitcher!!, gameStep.homePitcherName, gameStep.awayBatter!!, gameStep.awayBatterName!!))
                        else if (gameStep.awayPitcher != null) _onHit.emit(BlaseballHitEvent(event, gameStep, gameStep.awayPitcher!!, gameStep.awayPitcherName!!, gameStep.homeBatter!!, gameStep.homeBatterName!!))
                    }
                }
                is BlaseballFeedEvent.StolenBase -> if (gameStep != null) _onStolenBase.emit(BlaseballStolenBaseEvent(event, gameStep))
                is BlaseballFeedEvent.Sun2InGame -> if (gameStep != null) _onSun2.emit(BlaseballSun2Event(event, gameStep))
                is BlaseballFeedEvent.Flooding -> if (gameStep != null) _onFlood.emit(BlaseballFloodingEvent(event, gameStep, gameStep.baseRunners.filterNot(event.playerTags::contains)))
                else -> {
                }
            }
        }
    }

    val onGameEnd: SharedFlow<BlaseballGameEndEvent> by this::_onGameEnd
    val onShutout: SharedFlow<BlaseballShutoutEvent> by this::_onShutout
    val onBlackHole: SharedFlow<BlaseballBlackHoleEvent> by this::_onBlackHole
    val onIncineration: SharedFlow<BlaseballIncinerationEvent> by this::_onIncineration
    val onStrikeout: SharedFlow<BlaseballStrikeoutEvent> by this::_onStrikeout
    val onTeamShamed: SharedFlow<BlaseballTeamShamedEvent> by this::_onTeamShamed
    val onTeamShames: SharedFlow<BlaseballTeamShamesEvent> by this::_onTeamShames
    val onHomeRun: SharedFlow<BlaseballHomeRunEvent> by this::_onHomeRun
    val onHit: SharedFlow<BlaseballHitEvent> by this::_onHit
    val onStolenBase: SharedFlow<BlaseballStolenBaseEvent> by this::_onStolenBase
    val onSun2: SharedFlow<BlaseballSun2Event> by this::_onSun2
    val onFlood: SharedFlow<BlaseballFloodingEvent> by this::_onFlood
}