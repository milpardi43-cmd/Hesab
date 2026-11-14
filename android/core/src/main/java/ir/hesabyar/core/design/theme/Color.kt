package ir.hesabyar.core.design.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Brand palette — deep green communicates trust and money, light green income, red expense.
val DeepGreen = Color(0xFF0E5A43)
val DeepGreenDark = Color(0xFF083D2D)
val Mint = Color(0xFF22A06B)
val IncomeGreen = Color(0xFF1F9D62)
val ExpenseRed = Color(0xFFD64545)
val WarningOrange = Color(0xFFE08A00)
val PaperWhite = Color(0xFFFFFFFF)
val MistGray = Color(0xFFF4F6F3)
val SlateText = Color(0xFF1C2421)
val SecondaryText = Color(0xFF5C6661)
val Hairline = Color(0xFFE1E6E1)

val DarkBackground = Color(0xFF0E1411)
val DarkSurface = Color(0xFF18211D)
val DarkSurfaceVariant = Color(0xFF243029)
val DarkHairline = Color(0xFF2E3B34)
val DarkPrimary = Color(0xFF5BC99B)
val DarkIncome = Color(0xFF4ADE80)
val DarkExpense = Color(0xFFF87171)
val DarkWarning = Color(0xFFFBBF24)

@Immutable
data class HesabyarColors(
    val income: Color,
    val expense: Color,
    val warning: Color,
    val receivable: Color,
    val payable: Color,
    val neutral: Color,
    val onSurfaceSecondary: Color,
    val hairline: Color,
    val cardSurface: Color,
    val positiveContainer: Color,
    val negativeContainer: Color,
    val warningContainer: Color
)

val LightHesabyarColors = HesabyarColors(
    income = IncomeGreen,
    expense = ExpenseRed,
    warning = WarningOrange,
    receivable = Color(0xFF16794F),
    payable = Color(0xFFB43B3B),
    neutral = SecondaryText,
    onSurfaceSecondary = SecondaryText,
    hairline = Hairline,
    cardSurface = PaperWhite,
    positiveContainer = Color(0xFFE7F6EE),
    negativeContainer = Color(0xFFFDECEC),
    warningContainer = Color(0xFFFFF4E0)
)

val DarkHesabyarColors = HesabyarColors(
    income = DarkIncome,
    expense = DarkExpense,
    warning = DarkWarning,
    receivable = Color(0xFF6EE7B7),
    payable = Color(0xFFFCA5A5),
    neutral = Color(0xFF9BA7A1),
    onSurfaceSecondary = Color(0xFFA7B3AD),
    hairline = DarkHairline,
    cardSurface = DarkSurface,
    positiveContainer = Color(0xFF143527),
    negativeContainer = Color(0xFF3A1D1D),
    warningContainer = Color(0xFF3A2E14)
)

val LightColors = lightColorScheme(
    primary = DeepGreen,
    onPrimary = PaperWhite,
    primaryContainer = Color(0xFFD9F2E6),
    onPrimaryContainer = DeepGreenDark,
    secondary = Mint,
    onSecondary = PaperWhite,
    secondaryContainer = Color(0xFFE2F5EB),
    onSecondaryContainer = Color(0xFF0B4A36),
    tertiary = ExpenseRed,
    onTertiary = PaperWhite,
    background = MistGray,
    onBackground = SlateText,
    surface = PaperWhite,
    onSurface = SlateText,
    surfaceVariant = Color(0xFFEDF1EC),
    onSurfaceVariant = SecondaryText,
    outline = Hairline,
    outlineVariant = Color(0xFFEBEFEA),
    error = ExpenseRed,
    onError = PaperWhite,
    surfaceContainerLow = Color(0xFFFAFBF9),
    surfaceContainer = PaperWhite,
    surfaceContainerHigh = PaperWhite
)

val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF05231A),
    primaryContainer = Color(0xFF10382B),
    onPrimaryContainer = Color(0xFFB7F0D5),
    secondary = Color(0xFF34C08B),
    onSecondary = Color(0xFF04130D),
    secondaryContainer = Color(0xFF123326),
    onSecondaryContainer = Color(0xFFA8EBD0),
    tertiary = DarkExpense,
    onTertiary = Color(0xFF2A0A0A),
    background = DarkBackground,
    onBackground = Color(0xFFE6EDE8),
    surface = DarkSurface,
    onSurface = Color(0xFFE6EDE8),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFA7B3AD),
    outline = DarkHairline,
    outlineVariant = Color(0xFF2A362F),
    error = DarkExpense,
    onError = Color(0xFF2A0A0A),
    surfaceContainerLow = Color(0xFF121A16),
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = Color(0xFF1E2923)
)
