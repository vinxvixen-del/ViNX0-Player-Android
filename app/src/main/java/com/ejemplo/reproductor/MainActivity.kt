package com.ejemplo.reproductor

import android.Manifest
import android.content.ComponentName
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF121217)),
                contentAlignment = Alignment.Center
            ) {
                VerificarPermisos()
            }
        }
    }
}

@Composable
fun VerificarPermisos() {
    val contexto = LocalContext.current
    var permisosOtorgados by remember { mutableStateOf(false) }

    val listaPermisos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val lanzadorPermisos = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        permisosOtorgados = resultado.values.all { it }
        if (!permisosOtorgados) {
            Toast.makeText(contexto, "Se necesitan permisos para ver tus archivos locales", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        lanzadorPermisos.launch(listaPermisos)
    }

    if (permisosOtorgados) {
        CargarReproductor()
    } else {
        Text("Concede acceso a archivos para usar el reproductor", color = Color.LightGray, fontSize = 16.sp)
    }
}

@Composable
fun CargarReproductor() {
    val contexto = LocalContext.current
    val controlador = remember { mutableStateOf<MediaController?>(null) }
    val nombreArchivo = remember { mutableStateOf("Cargando archivos...") }

    LaunchedEffect(Unit) {
        val tokenSesion = SessionToken(contexto, ComponentName(contexto, PlaybackService::class.java))
        val futuroControlador = MediaController.Builder(contexto, tokenSesion).buildAsync()

        futuroControlador.addListener({
            val ctrl = futuroControlador.get()
            val listaArchivos = ObtenerArchivosLocales()

            if (listaArchivos.isNotEmpty() && ctrl.mediaItemCount == 0) {
                ctrl.setMediaItems(listaArchivos)
                ctrl.repeatMode = Player.REPEAT_MODE_ALL
                ctrl.prepare()
            }

            ctrl.addListener(object : Player.Listener {
                override fun onMediaMetadataChanged(meta: MediaMetadata) {
                    nombreArchivo.value = meta.title?.toString() ?: "Archivo multimedia"
                }
            })

            controlador.value = ctrl
        }, MoreExecutors.directExecutor())
    }

    DisposableEffect(Unit) {
        onDispose { controlador.value?.release() }
    }

    controlador.value?.let { reproductor ->
        InterfazCompleta(reproductor, nombreArchivo.value)
    } ?: Text("Buscando tus archivos...", color = Color.LightGray)
}

@OptIn(UnstableApi::class)
@Composable
fun InterfazCompleta(reproductor: Player, titulo: String) {
    var posicion by remember { mutableStateOf(0L) }
    var duracionTotal by remember { mutableStateOf(0L) }
    var estaMoviendoBarra by remember { mutableStateOf(false) }

    var mostrarEcualizador by remember { mutableStateOf(false) }
    var eq60Hz by remember { mutableStateOf(3f) }
    var eq230Hz by remember { mutableStateOf(1f) }
    var eq910Hz by remember { mutableStateOf(-2f) }
    var eq4kHz by remember { mutableStateOf(2f) }
    var eq14kHz by remember { mutableStateOf(5f) }
    var bassBoost by remember { mutableStateOf(40f) }
    var virtualizer by remember { mutableStateOf(30f) }

    LaunchedEffect(reproductor) {
        while (true) {
            if (!estaMoviendoBarra) {
                posicion = reproductor.currentPosition
                duracionTotal = reproductor.duration.coerceAtLeast(0L)
            }
            delay(400)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Área de video o vista
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color.Black, shape = RoundedCornerShape(18.dp))
        ) {
            AndroidView(
                factory = { vista ->
                    PlayerView(vista).apply {
                        player = reproductor
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (mostrarEcualizador) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ecualizador & Efectos", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { mostrarEcualizador = false },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Cerrar", fontSize = 11.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5 Band sliders
            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val bandas = listOf(
                    "60Hz" to eq60Hz,
                    "230Hz" to eq230Hz,
                    "910Hz" to eq910Hz,
                    "4kHz" to eq4kHz,
                    "14kHz" to eq14kHz
                )
                bandas.forEachIndexed { index, pair ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("${pair.second.toInt()}dB", color = Color(0xFF818CF8), fontSize = 10.sp)
                        Slider(
                            value = pair.second,
                            onValueChange = {
                                when (index) {
                                    0 -> eq60Hz = it
                                    1 -> eq230Hz = it
                                    2 -> eq910Hz = it
                                    3 -> eq4kHz = it
                                    4 -> eq14kHz = it
                                }
                            },
                            valueRange = -15f..15f,
                            modifier = Modifier.height(80.dp).width(24.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF6366F1),
                                activeTrackColor = Color(0xFF6366F1),
                                inactiveTrackColor = Color(0xFF27272A)
                            )
                        )
                        Text(pair.first, color = Color.Gray, fontSize = 9.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bass Boost & Virtualizer sliders
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Bass Boost (Refuerzo de graves)", color = Color.LightGray, fontSize = 12.sp)
                    Text("${bassBoost.toInt()}%", color = Color(0xFF818CF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = bassBoost,
                    onValueChange = { bassBoost = it },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF6366F1), activeTrackColor = Color(0xFF6366F1))
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Virtualizer (Efecto 3D)", color = Color.LightGray, fontSize = 12.sp)
                    Text("${virtualizer.toInt()}%", color = Color(0xFF818CF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = virtualizer,
                    onValueChange = { virtualizer = it },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF6366F1), activeTrackColor = Color(0xFF6366F1))
                )
            }
        } else {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = titulo,
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Barra de progreso
            Slider(
                value = if (duracionTotal > 0) posicion.toFloat() else 0f,
                valueRange = 0f..duracionTotal.coerceAtLeast(1L).toFloat(),
                onValueChange = {
                    posicion = it.toLong()
                    estaMoviendoBarra = true
                },
                onValueChangeFinished = {
                    estaMoviendoBarra = false
                    reproductor.seekTo(posicion)
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF6366F1),
                    activeTrackColor = Color(0xFF6366F1),
                    inactiveTrackColor = Color(0xFF3F3F46)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(FormatearTiempo(posicion), color = Color.Gray, fontSize = 13.sp)
                Text(FormatearTiempo(duracionTotal), color = Color.Gray, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botones de control
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { reproductor.seekToPreviousMediaItem() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A))
                ) {
                    Text("⏮", fontSize = 16.sp, color = Color.White)
                }

                Button(
                    onClick = { if (reproductor.isPlaying) reproductor.pause() else reproductor.play() },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    modifier = Modifier.height(56.dp).padding(horizontal = 12.dp)
                ) {
                    Text(if (reproductor.isPlaying) "⏸ Pausa" else "▶ Reproducir", fontSize = 17.sp)
                }

                Button(
                    onClick = { reproductor.seekToNextMediaItem() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A))
                ) {
                    Text("⏭", fontSize = 16.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { mostrarEcualizador = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A)),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Text("🎚️ Abrir Ecualizador & Efectos", fontSize = 14.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("ViNX0 Reproductor • Solo archivos locales", color = Color.Gray, fontSize = 12.sp)
    }
}

fun FormatearTiempo(miliseg: Long): String {
    if (miliseg <= 0) return "00:00"
    val totalSeg = miliseg / 1000
    val minutos = totalSeg / 60
    val segundos = totalSeg % 60
    return String.format("%02d:%02d", minutos, segundos)
}

fun ObtenerArchivosLocales(): List<MediaItem> {
    val lista = mutableListOf<MediaItem>()
    val formatosValidos = listOf("mp3", "mp4", "m4a", "wav", "ogg", "mkv", "flac", "aac", "avi")

    // Busca en varias carpetas principales
    val carpetasBuscar = listOf(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
    )

    carpetasBuscar.forEach { carpeta ->
        if (carpeta.exists() && carpeta.isDirectory) {
            carpeta.listFiles()?.forEach { archivo ->
                if (archivo.extension.lowercase() in formatosValidos) {
                    val datos = MediaMetadata.Builder()
                        .setTitle(archivo.nameWithoutExtension)
                        .build()
                    lista.add(
                        MediaItem.Builder()
                            .setUri(Uri.fromFile(archivo))
                            .setMediaMetadata(datos)
                            .build()
                    )
                }
            }
        }
    }
    return lista
}
