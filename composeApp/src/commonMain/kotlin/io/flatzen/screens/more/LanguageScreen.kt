package io.flatzen.screens.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.back
import flatzen.composeapp.generated.resources.language_english
import flatzen.composeapp.generated.resources.language_georgian
import flatzen.composeapp.generated.resources.language_kazakh
import flatzen.composeapp.generated.resources.language_polish
import flatzen.composeapp.generated.resources.language_russian
import flatzen.composeapp.generated.resources.language_spanish
import flatzen.composeapp.generated.resources.language_system
import flatzen.composeapp.generated.resources.language_title
import io.flatzen.commoncomponents.theme.AppLanguage
import io.flatzen.di.container
import io.flatzen.localization.LocalAppLanguage
import io.flatzen.viewmodel.more.MoreContainer
import io.flatzen.viewmodel.more.MoreIntent
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import repository.userpreferences.UserPreferencesRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen() {
    val moreContainer: MoreContainer = container()
    val userPreferences: UserPreferencesRepository = koinInject()
    val appLanguage = LocalAppLanguage.current
    val scope = rememberCoroutineScope()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        stringResource(Res.string.language_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { moreContainer.store.intent(MoreIntent.NavigateBack) },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 24.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Enum order: SYSTEM, EN, RU, PL, KK, KA
                    AppLanguage.entries.forEach { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (language != appLanguage) {
                                        scope.launch { userPreferences.setAppLanguage(language) }
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = appLanguage == language,
                                onClick = {
                                    if (language != appLanguage) {
                                        scope.launch { userPreferences.setAppLanguage(language) }
                                    }
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(language.labelRes()),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

internal fun AppLanguage.labelRes(): StringResource = when (this) {
    AppLanguage.SYSTEM -> Res.string.language_system
    AppLanguage.EN -> Res.string.language_english
    AppLanguage.ES -> Res.string.language_spanish
    AppLanguage.RU -> Res.string.language_russian
    AppLanguage.PL -> Res.string.language_polish
    AppLanguage.KK -> Res.string.language_kazakh
    AppLanguage.KA -> Res.string.language_georgian
}
