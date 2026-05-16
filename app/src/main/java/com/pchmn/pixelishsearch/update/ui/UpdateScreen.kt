package com.pchmn.pixelishsearch.update.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pchmn.pixelishsearch.PixelishSearchApp
import com.pchmn.pixelishsearch.R
import com.pchmn.pixelishsearch.update.data.UpdateInstaller
import kotlinx.coroutines.launch
import java.io.File

private sealed interface DownloadState {
    object Idle : DownloadState
    data class Downloading(val progress: Float) : DownloadState
    object Failed : DownloadState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as PixelishSearchApp
    val scope = rememberCoroutineScope()

    val info by app.updates.available.collectAsStateWithLifecycle()
    val currentVersion = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }

    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }
    var canInstall by remember { mutableStateOf(UpdateInstaller.canInstall(context)) }

    // Refresh the install permission on every resume — the user may have just
    // come back from the "Install unknown apps" settings page.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canInstall = UpdateInstaller.canInstall(context)
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // If the APK is already downloaded and the user just granted the install
    // permission, fire the system installer immediately.
    LaunchedEffect(canInstall, downloadedApk) {
        val apk = downloadedApk
        if (canInstall && apk != null && downloadState is DownloadState.Idle) {
            UpdateInstaller.install(context, apk)
        }
    }

    // No update available (race: user closed the badge state after we checked)
    // Just finish.
    LaunchedEffect(info) {
        if (info == null) onBack()
    }
    val update = info ?: return

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.update_title),
                        fontWeight = FontWeight.Medium,
                    )
                },
                navigationIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledIconButton(
                            onClick = onBack,
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            ) {
                VersionsBlock(
                    currentVersion = currentVersion,
                    newVersion = update.versionName,
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.update_changelog_header),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = update.changelog.ifBlank { "—" },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                    )
                }

                Spacer(Modifier.height(24.dp))

                if (!canInstall && downloadedApk != null) {
                    PermissionNotice()
                }
            }

            ActionBar(
                state = downloadState,
                canInstall = canInstall,
                hasDownloadedApk = downloadedApk != null,
                onCancel = onBack,
                onPrimaryClick = {
                    val apk = downloadedApk
                    if (apk != null) {
                        if (canInstall) {
                            UpdateInstaller.install(context, apk)
                        } else {
                            UpdateInstaller.requestInstallPermission(context)
                        }
                        return@ActionBar
                    }
                    downloadState = DownloadState.Downloading(0f)
                    scope.launch {
                        val file = UpdateInstaller.download(
                            context = context,
                            url = update.downloadUrl,
                            onProgress = { progress ->
                                downloadState = DownloadState.Downloading(progress)
                            },
                        )
                        if (file != null) {
                            downloadedApk = file
                            downloadState = DownloadState.Idle
                            if (UpdateInstaller.canInstall(context)) {
                                UpdateInstaller.install(context, file)
                            } else {
                                UpdateInstaller.requestInstallPermission(context)
                            }
                        } else {
                            downloadState = DownloadState.Failed
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun VersionsBlock(currentVersion: String, newVersion: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.update_current_version, currentVersion),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.update_new_version, newVersion),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PermissionNotice() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.update_permission_required_title),
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.update_permission_required_subtitle),
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun ActionBar(
    state: DownloadState,
    canInstall: Boolean,
    hasDownloadedApk: Boolean,
    onCancel: () -> Unit,
    onPrimaryClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        if (state is DownloadState.Downloading) {
            LinearProgressIndicator(
                progress = { state.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )
            Text(
                text = stringResource(R.string.update_status_downloading),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        } else if (state is DownloadState.Failed) {
            Text(
                text = stringResource(R.string.update_status_failed),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        val isDownloading = state is DownloadState.Downloading
        val primaryLabel = when {
            hasDownloadedApk && !canInstall -> stringResource(R.string.update_action_grant_permission)
            else -> stringResource(R.string.update_action_install)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel, enabled = !isDownloading) {
                Text(stringResource(R.string.update_action_cancel))
            }
            Button(
                onClick = onPrimaryClick,
                enabled = !isDownloading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(primaryLabel)
            }
        }
    }
}
