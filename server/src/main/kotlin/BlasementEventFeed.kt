import dev.brella.blasement.common.events.*
import dev.brella.kornea.blaseball.base.common.beans.BlaseballFeedEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class BlasementEventFeed(val flow: SharedFlow<BlaseballFeedEventWithContext>, scope: CoroutineScope, context: CoroutineContext = scope.coroutineContext) {
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
        flow.collect { event ->
            val (subEvent, gameStep) = event
            when (subEvent) {
                is BlaseballFeedEvent.GameEndLog -> if (gameStep != null) {
                    _onGameEnd.emit(BlaseballGameEndEvent(subEvent, gameStep, subEvent.metadata.winner))

                    //TODO: Triple check if this is valid shutout logic
                    if (gameStep.homePitcher != null && gameStep.awayScore == 0.0) {
                        _onShutout.emit(BlaseballShutoutEvent(subEvent, gameStep, gameStep.homePitcher!!, gameStep.homePitcherName!!))
                    } else if (gameStep.awayPitcher != null && gameStep.homeScore == 0.0) {
                        _onShutout.emit(BlaseballShutoutEvent(subEvent, gameStep, gameStep.awayPitcher!!, gameStep.awayPitcherName!!))
                    }
                } else println("Missing gameStep for $subEvent")
                is BlaseballFeedEvent.BlackHoleInGame -> if (gameStep != null) _onBlackHole.emit(BlaseballBlackHoleEvent(subEvent, gameStep))
                //Incineration technically has two events, so we should listen to the parent one - that has more information
                is BlaseballFeedEvent.Incineration -> if (gameStep != null && subEvent.metadata.children != null) _onIncineration.emit(BlaseballIncinerationEvent(subEvent, gameStep))
                is BlaseballFeedEvent.Strikeout -> if (gameStep != null) _onStrikeout.emit(BlaseballStrikeoutEvent(subEvent, gameStep))
                is BlaseballFeedEvent.TeamShamed -> _onTeamShamed.emit(BlaseballTeamShamedEvent(subEvent, subEvent.teamTags.first()))
                is BlaseballFeedEvent.TeamShames -> _onTeamShames.emit(BlaseballTeamShamesEvent(subEvent, subEvent.teamTags.first()))
                is BlaseballFeedEvent.HomeRun -> if (gameStep != null) _onHomeRun.emit(BlaseballHomeRunEvent(subEvent, gameStep))
                is BlaseballFeedEvent.Hit -> {
                    if (gameStep != null) {
                        if (gameStep.homePitcher != null && gameStep.awayBatter != null) _onHit.emit(BlaseballHitEvent(subEvent, gameStep, gameStep.homePitcher!!, gameStep.homePitcherName, gameStep.awayBatter!!, gameStep.awayBatterName!!))
                        else if (gameStep.awayPitcher != null && gameStep.homeBatter != null) _onHit.emit(BlaseballHitEvent(subEvent, gameStep, gameStep.awayPitcher!!, gameStep.awayPitcherName!!, gameStep.homeBatter!!, gameStep.homeBatterName!!))
                    }
                }
                is BlaseballFeedEvent.StolenBase -> if (gameStep != null) _onStolenBase.emit(BlaseballStolenBaseEvent(subEvent, gameStep))
                is BlaseballFeedEvent.Sun2InGame -> if (gameStep != null) _onSun2.emit(BlaseballSun2Event(subEvent, gameStep))
                is BlaseballFeedEvent.Flooding -> if (gameStep != null) _onFlood.emit(BlaseballFloodingEvent(subEvent, gameStep, gameStep.baseRunners.filterNot(subEvent.playerTags::contains)))
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