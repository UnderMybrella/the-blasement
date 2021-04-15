import dev.brella.blasement.common.events.BlaseballFeedEventWithContext
import dev.brella.kornea.blaseball.BlaseballFeedEventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class BlasementEventFeed(val feed: BlaseballFeed, scope: CoroutineScope, context: CoroutineContext) {
    private val _onGameEnd: MutableSharedFlow<BlaseballFeedEventWithContext> = MutableSharedFlow()
    private val _onBlackHole: MutableSharedFlow<BlaseballFeedEventWithContext> = MutableSharedFlow()
    private val _onIncineration: MutableSharedFlow<BlaseballFeedEventWithContext> = MutableSharedFlow()
    private val _onStrikeout: MutableSharedFlow<BlaseballFeedEventWithContext> = MutableSharedFlow()
    private val _onTeamShamed: MutableSharedFlow<BlaseballFeedEventWithContext> = MutableSharedFlow()
    private val _onTeamShames: MutableSharedFlow<BlaseballFeedEventWithContext> = MutableSharedFlow()
    private val _onHomeRun: MutableSharedFlow<BlaseballFeedEventWithContext> = MutableSharedFlow()
    private val _onHit: MutableSharedFlow<BlaseballFeedEventWithContext> = MutableSharedFlow()
    private val _onStolenBase: MutableSharedFlow<BlaseballFeedEventWithContext> = MutableSharedFlow()
    private val _onSun2: MutableSharedFlow<BlaseballFeedEventWithContext> = MutableSharedFlow()
    private val _onFlood: MutableSharedFlow<BlaseballFeedEventWithContext> = MutableSharedFlow()

    private val collector = scope.launch(context) {
        BlaseballFeedEventType.apply {
//            listOf(.GAME_END_LOG)
        }
    }

    val onGameEnd: SharedFlow<BlaseballFeedEventWithContext> by this::_onGameEnd
    val onBlackHole: SharedFlow<BlaseballFeedEventWithContext> by this::_onBlackHole
    val onIncineration: SharedFlow<BlaseballFeedEventWithContext> by this::_onIncineration
    val onStrikeout: SharedFlow<BlaseballFeedEventWithContext> by this::_onStrikeout
    val onTeamShamed: SharedFlow<BlaseballFeedEventWithContext> by this::_onTeamShamed
    val onTeamShames: SharedFlow<BlaseballFeedEventWithContext> by this::_onTeamShames
    val onHomeRun: SharedFlow<BlaseballFeedEventWithContext> by this::_onHomeRun
    val onHit: SharedFlow<BlaseballFeedEventWithContext> by this::_onHit
    val onStolenBase: SharedFlow<BlaseballFeedEventWithContext> by this::_onStolenBase
    val onSun2: SharedFlow<BlaseballFeedEventWithContext> by this::_onSun2
    val onFlood: SharedFlow<BlaseballFeedEventWithContext> by this::_onFlood
}