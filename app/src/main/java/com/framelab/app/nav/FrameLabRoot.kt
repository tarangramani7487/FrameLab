package com.framelab.app.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.framelab.app.data.AppContainer
import com.framelab.app.feature.editor.EditorScreen
import com.framelab.app.feature.export.PhotoAccessOffScreen
import com.framelab.app.feature.export.SuccessScreen
import com.framelab.app.feature.home.CreateSheet
import com.framelab.app.feature.home.ExploreScreen
import com.framelab.app.feature.home.HomeScreen
import com.framelab.app.feature.home.SeeAllScreen
import com.framelab.app.feature.home.TemplatePreviewSheet
import com.framelab.app.feature.library.MyDesignsScreen
import com.framelab.app.feature.library.PostDetailScreen
import com.framelab.app.feature.onboarding.OnboardingScreen
import com.framelab.app.feature.onboarding.PrimerScreen
import com.framelab.app.feature.onboarding.SplashScreen
import com.framelab.app.feature.profile.AboutScreen
import com.framelab.app.feature.profile.DeleteAccountScreen
import com.framelab.app.feature.profile.EditProfileScreen
import com.framelab.app.feature.profile.FeedbackScreen
import com.framelab.app.feature.profile.HelpScreen
import com.framelab.app.feature.profile.InboxScreen
import com.framelab.app.feature.profile.LicencesScreen
import com.framelab.app.feature.profile.PaywallScreen
import com.framelab.app.feature.profile.ProfileScreen
import com.framelab.app.feature.profile.SettingsScreen
import com.framelab.app.feature.profile.SignInEmailScreen
import com.framelab.app.feature.profile.SignInScreen
import com.framelab.app.feature.profile.WelcomeProScreen
import com.framelab.app.ui.components.ProvideToaster
import com.framelab.app.ui.components.Tab
import com.framelab.app.ui.components.TabBar
import com.framelab.app.ui.components.ToastHost
import com.framelab.app.ui.components.Toaster
import com.framelab.app.ui.theme.Fl
import com.framelab.app.ui.theme.FlMotion
import com.framelab.app.ui.theme.FrameLabTheme

/** Route names (HANDOFF §6). */
object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val PRIMER = "primer"
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val DESIGNS = "designs?tab={tab}"
    const val PROFILE = "profile"
    const val SEE_ALL = "seeAll/{categoryId}?origin={origin}"
    const val EDITOR = "editor?templateId={templateId}&draftId={draftId}"
    const val SUCCESS = "success/{postId}"
    const val PHOTO_ACCESS = "photoAccess"
    const val POST = "post/{postId}"
    const val EDIT_PROFILE = "editProfile"
    const val SETTINGS = "settings"
    const val DELETE_ACCOUNT = "deleteAccount"
    const val LICENCES = "licences"
    const val HELP = "help"
    const val FEEDBACK = "feedback"
    const val ABOUT = "about"
    const val SIGN_IN = "signIn"
    const val SIGN_IN_EMAIL = "signInEmail"
    const val INBOX = "inbox/{email}"
    const val PAYWALL = "paywall"
    const val WELCOME_PRO = "welcomePro"

    fun designs(tab: Int = 0) = "designs?tab=$tab"
    fun seeAll(categoryId: String, origin: Tab = Tab.Home) = "seeAll/$categoryId?origin=${origin.name}"
    fun editor(templateId: String? = null, draftId: String? = null) =
        "editor?templateId=${templateId.orEmpty()}&draftId=${draftId.orEmpty()}"
    fun success(postId: String) = "success/$postId"
    fun post(postId: String) = "post/$postId"
    fun inbox(email: String) = "inbox/${android.net.Uri.encode(email)}"
}

/** App-wide UI state and navigation helpers. Read with [LocalAppState]. */
@Stable
class AppState(val nav: NavHostController, val container: AppContainer, val toaster: Toaster) {
    /** Template preview sheet (N7), shown over any screen. */
    var previewTemplateId by mutableStateOf<String?>(null)
    /** Create sheet (N6). */
    var createSheet by mutableStateOf(false)

    fun openPreview(templateId: String) { previewTemplateId = templateId }
    fun openEditor(templateId: String? = null, draftId: String? = null) {
        previewTemplateId = null; createSheet = false
        nav.navigate(Routes.editor(templateId, draftId))
    }
    fun navigate(route: String) = nav.navigate(route)
    fun back() { if (!nav.popBackStack()) Unit }

    /** Switches bottom tab, keeping one back stack entry per tab. */
    fun selectTab(tab: Tab) {
        when (tab) {
            Tab.Create -> { createSheet = true; return }
            else -> Unit
        }
        val route = when (tab) {
            Tab.Home -> Routes.HOME
            Tab.Explore -> Routes.EXPLORE
            Tab.Designs -> Routes.designs(0)
            Tab.Profile -> Routes.PROFILE
            Tab.Create -> return
        }
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    /** Leaves onboarding/splash and lands on Home with a clean back stack. */
    fun goHome() {
        nav.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } }
    }
}

val LocalAppState = staticCompositionLocalOf<AppState> { error("AppState not provided") }

private fun tabFor(route: String?, origin: String?): Tab? = when {
    route == null -> null
    route == Routes.HOME -> Tab.Home
    route == Routes.EXPLORE -> Tab.Explore
    route.startsWith("designs") -> Tab.Designs
    route == Routes.PROFILE -> Tab.Profile
    route.startsWith("seeAll") -> origin?.let { runCatching { Tab.valueOf(it) }.getOrNull() } ?: Tab.Home
    else -> null
}

@Composable
fun FrameLabRoot(container: AppContainer, startRoute: String? = null) {
    val nav = rememberNavController()
    val toaster = remember { Toaster() }
    val app = remember { AppState(nav, container, toaster) }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val tab = tabFor(route, entry?.arguments?.getString("origin"))

    androidx.compose.runtime.LaunchedEffect(startRoute) {
        if (!startRoute.isNullOrBlank()) {
            nav.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } }
            if (startRoute != Routes.HOME) nav.navigate(startRoute)
        }
    }

    FrameLabTheme {
        ProvideToaster(toaster) {
            androidx.compose.runtime.CompositionLocalProvider(LocalAppState provides app) {
                Box(Modifier.fillMaxSize().background(if (route == Routes.EDITOR) Fl.edBg else Fl.paper)) {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).fillMaxWidth()) { AppNavHost(nav) }
                        if (tab != null) TabBar(tab, app::selectTab)
                    }
                    if (route != Routes.EDITOR) {
                        ToastHost(
                            Modifier.align(Alignment.BottomCenter)
                                .then(if (tab == null) Modifier.windowInsetsPadding(WindowInsets.navigationBars) else Modifier)
                                .padding(start = 12.dp, end = 12.dp, bottom = if (tab != null) 64.dp + 12.dp + navInset() else 12.dp),
                        )
                    }
                    TemplatePreviewSheet(app.previewTemplateId) { app.previewTemplateId = null }
                    CreateSheet(app.createSheet) { app.createSheet = false }
                }
            }
        }
    }
}

@Composable
private fun navInset() = with(androidx.compose.ui.platform.LocalDensity.current) {
    WindowInsets.navigationBars.getBottom(this).toDp()
}

@Composable
private fun AppNavHost(nav: NavHostController) {
    NavHost(
        nav, startDestination = Routes.SPLASH,
        enterTransition = { fadeIn(tween(180)) }, exitTransition = { fadeOut(tween(120)) },
        popEnterTransition = { fadeIn(tween(180)) }, popExitTransition = { fadeOut(tween(120)) },
    ) {
        composable(Routes.SPLASH) { SplashScreen() }
        composable(Routes.ONBOARDING) { OnboardingScreen() }
        composable(Routes.PRIMER) { PrimerScreen() }
        composable(Routes.HOME) { HomeScreen() }
        composable(Routes.EXPLORE) { ExploreScreen() }
        composable(Routes.DESIGNS, arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 })) {
            MyDesignsScreen(it.arguments?.getInt("tab") ?: 0)
        }
        composable(Routes.PROFILE) { ProfileScreen() }
        composable(
            Routes.SEE_ALL,
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType }, navArgument("origin") { type = NavType.StringType; defaultValue = "Home" }),
        ) { SeeAllScreen(it.arguments?.getString("categoryId").orEmpty()) }
        composable(
            Routes.EDITOR,
            arguments = listOf(
                navArgument("templateId") { type = NavType.StringType; defaultValue = "" },
                navArgument("draftId") { type = NavType.StringType; defaultValue = "" },
            ),
            enterTransition = { slideInHorizontally(tween(FlMotion.EDITOR_PUSH)) { it } },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(120)) },
            popExitTransition = { slideOutHorizontally(tween(FlMotion.EDITOR_POP)) { it } },
        ) {
            EditorScreen(
                templateId = it.arguments?.getString("templateId")?.ifEmpty { null },
                draftId = it.arguments?.getString("draftId")?.ifEmpty { null },
            )
        }
        composable(Routes.SUCCESS) { SuccessScreen(it.arguments?.getString("postId").orEmpty()) }
        composable(Routes.PHOTO_ACCESS) { PhotoAccessOffScreen() }
        composable(Routes.POST) { PostDetailScreen(it.arguments?.getString("postId").orEmpty()) }
        composable(Routes.EDIT_PROFILE) { EditProfileScreen() }
        composable(Routes.SETTINGS) { SettingsScreen() }
        composable(Routes.DELETE_ACCOUNT) { DeleteAccountScreen() }
        composable(Routes.LICENCES) { LicencesScreen() }
        composable(Routes.HELP) { HelpScreen() }
        composable(Routes.FEEDBACK) { FeedbackScreen() }
        composable(Routes.ABOUT) { AboutScreen() }
        composable(Routes.SIGN_IN) { SignInScreen() }
        composable(Routes.SIGN_IN_EMAIL) { SignInEmailScreen() }
        composable(Routes.INBOX) { InboxScreen(android.net.Uri.decode(it.arguments?.getString("email").orEmpty())) }
        composable(Routes.PAYWALL) { PaywallScreen() }
        composable(Routes.WELCOME_PRO) { WelcomeProScreen() }
    }
}
