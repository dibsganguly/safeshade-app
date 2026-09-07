package com.safeshade.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.platform.PlanOffer
import com.safeshade.platform.PlayBilling
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.PilotLamp
import com.safeshade.ui.board.Readout
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

/** The three tiers, as the page names them. Matches `subscriptions.tier`. */
enum class PlanTier(val key: String, val label: String, val listPrice: String, val productId: String?) {
    FREE("free", "Free", "₹0", null),
    PLUS("plus", "Plus", "₹99 / month", PlayBilling.PRODUCT_PLUS_MONTHLY),
    PRO("pro", "Pro", "₹299 / month", PlayBilling.PRODUCT_PRO_MONTHLY);

    companion object {
        fun fromKey(key: String?): PlanTier = entries.firstOrNull { it.key == key } ?: FREE
    }
}

/** Everything the plan page draws. */
data class PlanUiState(
    val current: PlanTier = PlanTier.FREE,
    /** True when a developer override, not a subscription, set `current`. */
    val overridden: Boolean = false,
    /** Play's own offers, once queried; null until then. */
    val offers: List<PlanOffer>? = null,
    /** Why Play gave no offers, in its words. */
    val offersError: String? = null,
    /** The product being bought right now, or null. */
    val purchasing: String? = null,
    /** Play's answer to the last purchase, in its words. */
    val purchaseError: String? = null,
    val signedIn: Boolean = false,
    val showDeveloper: Boolean = false
)

/**
 * The cloud tiers.
 *
 * Three plates, the current one lit. Every price is Play's own when Play
 * has answered and the list price until then, and the button says which.
 * Choosing a plan hands the product to Play Billing and reports Play's
 * actual result — which, with no Console listing yet, is Play's own
 * "item not available" message, shown verbatim. Nothing here says
 * "subscribed" until the subscription row says so.
 */
@Composable
fun PlanScreen(
    state: PlanUiState,
    onChoose: (PlanTier) -> Unit,
    onOpenSignIn: () -> Unit,
    onSetOverride: ((PlanTier?) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground),
        contentPadding = PaddingValues(
            start = Spacing.gutter,
            end = Spacing.gutter,
            top = contentPadding.calculateTopPadding() + Spacing.sm,
            bottom = contentPadding.calculateBottomPadding() + Spacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item("title") {
            ScreenHeader(
                title = "Plans",
                subtitle = "Everything that keeps someone safe is free. The plans add the cloud around it.",
                onBack = onBack
            )
        }

        items(PlanTier.entries.size, key = { "tier-" + PlanTier.entries[it].key }) { index ->
            val tier = PlanTier.entries[index]
            val offer = state.offers?.firstOrNull { it.productId == tier.productId }
            TierPlate(
                tier = tier,
                current = tier == state.current,
                overridden = state.overridden && tier == state.current,
                price = offer?.formattedPrice?.let { "$it / month" } ?: tier.listPrice,
                priceFromPlay = offer != null,
                busy = state.purchasing == tier.productId,
                anyBusy = state.purchasing != null,
                signedIn = state.signedIn,
                onChoose = { onChoose(tier) },
                onOpenSignIn = onOpenSignIn
            )
        }

        val note = state.purchaseError ?: state.offersError
        if (note != null) {
            item("note") {
                BoardPlate(modifier = Modifier.fillMaxWidth()) {
                    Way(
                        name = "Google Play said",
                        state = LampState.ATTENTION,
                        stateLabel = "Reported",
                        detail = note,
                        icon = SafeShadeIcons.Alert02
                    )
                }
            }
        }

        if (state.showDeveloper && onSetOverride != null) {
            item("dev-heading") { SectionPlate(title = "Developer") }
            item("dev") {
                BoardPlate(modifier = Modifier.fillMaxWidth()) {
                    PlanTier.entries.forEachIndexed { i, tier ->
                        if (i > 0) Hairline()
                        val on = state.overridden && state.current == tier
                        Way(
                            name = "Act as ${tier.label}",
                            state = if (on) LampState.LIVE else LampState.OFF,
                            stateLabel = if (on) "On" else "Off",
                            detail = "Sets the tier on this phone only, without a subscription",
                            icon = SafeShadeIcons.Crown,
                            onClick = { onSetOverride(if (on) null else tier) }
                        )
                    }
                }
            }
        }
    }
}

/** One tier: name and price at the top, the lamp for the current one, what it includes, one button. */
@Composable
private fun TierPlate(
    tier: PlanTier,
    current: Boolean,
    overridden: Boolean,
    price: String,
    priceFromPlay: Boolean,
    busy: Boolean,
    anyBusy: Boolean,
    signedIn: Boolean,
    onChoose: () -> Unit,
    onOpenSignIn: () -> Unit
) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = tier.label, style = MaterialTheme.typography.headlineSmall, color = colors.ink)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = when {
                        current && overridden -> "Your plan, set by the developer switch"
                        current -> "Your plan"
                        else -> tier.oneLine
                    },
                    style = MaterialTheme.boardType.rowDetail,
                    color = colors.inkMuted
                )
            }
            Spacer(Modifier.padding(Spacing.sm))
            Column(horizontalAlignment = Alignment.End) {
                PilotLamp(state = if (current) LampState.LIVE else LampState.OFF, size = 22.dp, description = if (current) "Your plan" else "Not your plan")
                Spacer(Modifier.height(Spacing.sm))
                Readout(label = if (priceFromPlay) "Play price" else "List price", value = price, compact = true, horizontalAlignment = Alignment.End)
            }
        }
        Hairline()
        Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
            tier.includes.forEach { line ->
                Text(
                    text = "· $line",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.ink,
                    modifier = Modifier.padding(vertical = Spacing.xs)
                )
            }
        }
        if (tier.productId != null && !current) {
            Hairline()
            Column(modifier = Modifier.padding(Spacing.lg)) {
                if (!signedIn) {
                    BoardButton(
                        label = "Sign In to Choose ${tier.label}",
                        supporting = "A plan belongs to an account",
                        onClick = onOpenSignIn,
                        weight = ButtonWeight.SECONDARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    BoardButton(
                        label = if (busy) "Waiting for Google Play…" else "Choose ${tier.label}",
                        supporting = if (priceFromPlay) "Billed by Google Play" else "Google Play sets the final price",
                        onClick = onChoose,
                        weight = ButtonWeight.COMMIT,
                        enabled = !anyBusy,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private val PlanTier.oneLine: String
    get() = when (this) {
        PlanTier.FREE -> "The whole safety board, on this phone and one cloud copy"
        PlanTier.PLUS -> "For a family: the Circle on every phone, alerts by email, the community map"
        PlanTier.PRO -> "For carers and larger households"
    }

private val PlanTier.includes: List<String>
    get() = when (this) {
        PlanTier.FREE -> listOf(
            "Fall detection, SOS, safe zones, check-ins and journeys",
            "A cloud copy of your wearers, zones and trip log",
            "One guardian on the account"
        )
        PlanTier.PLUS -> listOf(
            "Everything in Free",
            "Up to five guardians in the Circle, invited by email",
            "Alerts emailed to the Circle with a map link",
            "The community map of where alerts happen"
        )
        PlanTier.PRO -> listOf(
            "Everything in Plus",
            "Any number of guardians",
            "Voice notes between Circle phones kept for ninety days"
        )
    }

@Preview(name = "Plans", showBackground = true)
@Composable
private fun PlanPreview() {
    SafeShadeTheme {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            PlanScreen(
                state = PlanUiState(current = PlanTier.FREE, signedIn = true, purchaseError = "This item is not available for purchase", showDeveloper = true),
                onChoose = {}, onOpenSignIn = {}, onSetOverride = {}, onBack = {}
            )
        }
    }
}
