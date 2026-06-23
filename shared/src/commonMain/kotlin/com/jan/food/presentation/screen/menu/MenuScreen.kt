package com.jan.food.presentation.screen.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jan.food.domain.model.Allergen
import com.jan.food.presentation.components.button.BackButton
import com.jan.food.presentation.components.button.TapPulseState
import com.jan.food.presentation.components.grid.ReactiveGrid
import com.jan.food.presentation.components.pill.Pill
import org.koin.compose.viewmodel.koinViewModel

/**
 * The 14 allergens arranged as a diamond (`2-3-4-3-2`): short top/bottom rows, the widest row in
 * the middle, so the grid reads as a centred lozenge. The longest labels (Crustacean, Sulphites,
 * Tree nut…) are deliberately placed on the 2-pill rows and the shortest words fill the 4-pill
 * middle row, so every row fits within a narrow screen (e.g. iPhone mini).
 */
private val AllergenRows: List<List<Allergen>> = listOf(
    listOf(Allergen.TREE_NUT, Allergen.MOLLUSC),
    listOf(Allergen.PEANUT, Allergen.GLUTEN, Allergen.SESAME),
    listOf(Allergen.EGG, Allergen.SOY, Allergen.MILK, Allergen.FISH),
    listOf(Allergen.CELERY, Allergen.LUPIN, Allergen.MUSTARD),
    listOf(Allergen.CRUSTACEAN, Allergen.SULPHITES),
)

/** Title-cased, space-separated label for an allergen tag (e.g. `tree_nut` → `Tree nut`). */
private fun Allergen.label(): String =
    tag.replace('_', ' ').replaceFirstChar { it.uppercase() }

/**
 * Full-screen menu overlay drawn on top of the shared, blurred camera feed hosted by the NavHost:
 * a translucent white scrim with a [BackButton] in the same position as the home screen's menu
 * button. Reached via a fade transition from the home screen.
 *
 * @param onBackClick invoked when the back button is tapped.
 * @param viewModel the screen's [MenuScreenViewModel].
 */
@Composable
fun MenuScreen(
    onBackClick: () -> Unit,
    backButtonPulse: TapPulseState? = null,
    viewModel: MenuScreenViewModel = koinViewModel(),
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.5f)),
    )

    Box(modifier = Modifier.fillMaxSize().displayCutoutPadding()) {
        BackButton(
            onClick = onBackClick,
            // Shared with the home menu button so the two pulse in sync across navigation.
            pulse = backButtonPulse,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 16.dp),
        )

        ReactiveGrid(
            items = AllergenRows.map { row ->
                row.map { allergen ->
                    @Composable {
                        Pill {
                            Text(
                                text = allergen.label(),
                                color = Color.Black,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            },
            onItemClick = { _, _ -> /* TODO: toggle the tapped allergen once selection is wired. */ },
            horizontalSpacing = 10.dp,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
