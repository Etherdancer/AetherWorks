package org.example.aetherworks.ui.utilities

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.tileprovider.modules.OfflineTileProvider
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.views.overlay.Marker
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.util.GeoPoint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineMapsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Initialize osmdroid configuration
    Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

    val coroutineScope = remember { CoroutineScope(Dispatchers.Main) }
    var isLoadingMap by remember { androidx.compose.runtime.mutableStateOf(false) }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            
            // Enable My Location overlay
            val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), this)
            locationOverlay.enableMyLocation()
            overlays.add(locationOverlay)

            // POI Marking
            val mReceive = object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false
                override fun longPressHelper(p: GeoPoint?): Boolean {
                    p?.let {
                        val marker = Marker(this@apply)
                        marker.position = it
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        marker.title = "POI Marked"
                        marker.snippet = "Lat: ${it.latitude}, Lon: ${it.longitude}"
                        overlays.add(marker)
                        invalidate()
                    }
                    return true
                }
            }
            overlays.add(MapEventsOverlay(mReceive))

            // Set default zoom and center (e.g., global view)
            controller.setZoom(3.0)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { mapUri ->
            isLoadingMap = true
            coroutineScope.launch {
                val cachedFile = withContext(Dispatchers.IO) {
                    val destFile = File(context.cacheDir, "offline_map.sqlite")
                    context.contentResolver.openInputStream(mapUri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    destFile
                }
                
                try {
                    val provider = OfflineTileProvider(SimpleRegisterReceiver(context), arrayOf(cachedFile))
                    mapView.tileProvider = provider
                    mapView.invalidate()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                isLoadingMap = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView.onDetach()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(if (isLoadingMap) "Loading Offline Map..." else "Offline Maps") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { launcher.launch(arrayOf("application/vnd.sqlite3", "application/octet-stream", "*/*")) }) {
                Icon(Icons.Default.Map, contentDescription = "Load Map File")
            }
        }
    ) { paddingValues ->
        AndroidView(
            factory = { mapView },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}
