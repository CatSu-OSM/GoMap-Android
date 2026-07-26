package org.gomap.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import org.gomap.android.features.map.GoMapMapScreen
import org.gomap.android.features.map.MapViewModel
import org.gomap.android.ui.theme.GoMapAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GoMapAndroidTheme {
                val viewModel: MapViewModel = viewModel()
                var hasLocationPermission by remember {
                    mutableStateOf(checkLocationPermission())
                }
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { result ->
                    hasLocationPermission = result.values.any { granted -> granted }
                    if (hasLocationPermission) {
                        viewModel.centerOnLastKnownLocation(this)
                    }
                }

                LaunchedEffect(hasLocationPermission) {
                    if (hasLocationPermission) {
                        viewModel.centerOnLastKnownLocation(this@MainActivity)
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }

                GoMapMapScreen(
                    state = viewModel.uiState,
                    hasLocationPermission = hasLocationPermission,
                    onMapLongPress = viewModel::dropDraftNode,
                    onCenterOnUser = { viewModel.centerOnLastKnownLocation(this) },
                    onViewportChanged = viewModel::onViewportChanged,
                    onGrantLocation = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onLoadCurrentViewport = viewModel::loadCurrentViewport,
                    onFeatureSelected = viewModel::selectFeature,
                    onFeatureMoved = viewModel::moveSelectedFeature,
                    onFeatureTagsChanged = viewModel::updateSelectedFeatureTags,
                    onUndo = viewModel::undo,
                    onRedo = viewModel::redo,
                    onUpload = viewModel::requestUpload
                )
            }
        }
    }

    private fun checkLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }
}
