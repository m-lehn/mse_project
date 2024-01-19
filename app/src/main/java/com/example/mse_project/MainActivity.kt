package com.example.mse_project

//import kotlinx.coroutines.flow.internal.NopCollector
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.sharp.*
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.mse_project.TensorFLowHelper.imageSize
import com.example.mse_project.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Objects
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MSE_ProjectTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MyScreen()
                }
            }
        }
    }
}


@OptIn(ExperimentalCoilApi::class)
@Composable
fun MyScreen() {

    //val scope = rememberCoroutineScope()
    //var appState by remember { mutableStateOf(AppState()) }
    //val configuration = LocalConfiguration.current
    //val screenHeight = configuration.screenHeightDp.dp
    //val screenWidth = configuration.screenWidthDp.dp
    val context = LocalContext.current
    val file = context.createImageFile()
    val uri = FileProvider.getUriForFile(
        Objects.requireNonNull(context),
        BuildConfig.APPLICATION_ID + ".provider", file
    )

    //TODO: Placeholder Image not working

    var firstPicture by rememberSaveable {
        mutableStateOf<Boolean>(false)
    }

    var capturedImageUri by rememberSaveable {
        mutableStateOf<Uri>(Uri.EMPTY)
        //mutableStateOf<Uri>(Uri.fromFile(File("file:///android_asset/drawable/placeholder.png")))
    }

    var bitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(capturedImageUri) {
        if (capturedImageUri != Uri.EMPTY) {
            coroutineScope.launch {
                val imageLoader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(capturedImageUri)
                    .build()

                val result = (imageLoader.execute(request) as SuccessResult).drawable
                bitmap = (result as BitmapDrawable).bitmap
            }
        }
        onDispose {
            // Clean up resources if needed
        }
    }

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
            capturedImageUri = uri
        }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            Toast.makeText(context, "Permission Granted", Toast.LENGTH_SHORT).show()
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBarTitle = "Shrooming - The App",
        onFabClick = {

            val permissionCheckResult =
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
            if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                cameraLauncher.launch(uri)
                firstPicture = true
            } else {
                // Request a permission
                permissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
            //appState = AppState()
            //capturedImageUri = Uri.EMPTY
        }
    ) {

    }
    ImageArea(capturedImageUri)
    Spacer(modifier = Modifier.padding(20.dp))
    bitmap?.let {
        val scaledBitmap = Bitmap.createScaledBitmap(cropSquare(it), imageSize, imageSize, false);
        TensorFLowHelper.classifyImage(scaledBitmap) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(text = "Image is classified as:")
                Text(text = it, color = Color.White, fontSize = 24.sp)
            }
        }
    }
}

private fun cropSquare(bitmap: Bitmap): Bitmap {
    val size = min(bitmap.width, bitmap.height)
    val startX = (bitmap.width - size) / 2
    val startY = (bitmap.height - size) / 2
    return Bitmap.createBitmap(bitmap, startX, startY, size, size)
}

@Composable
fun ImageArea(
    capturedImageUri: Uri,
) {
    //paddingValues ->
    // TODO: Style
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val infiniteTransition = rememberInfiniteTransition(label = "infiniteTransition")
    val specialBorder by infiniteTransition.animateColor(
        initialValue = Color.White,
        targetValue = Color.Transparent,
        //targetValue = Color.argb(255, Random().nextInt(256), Random().nextInt(256), Random().nextInt(256)), //argb needs API>26
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ), label = "specialBorder"
    )
    Box(
        modifier = Modifier
            .clip(RectangleShape)
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedCard(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            border = BorderStroke(1.dp, specialBorder),
            modifier = Modifier
                .size(min(screenWidth, screenHeight))
                .clip(RectangleShape)
                .padding(15.dp)
                .padding(15.dp)
                .align(Alignment.Center)
        ) {

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(capturedImageUri)
                    .crossfade(true)
                    .build(),

                //placeholder = painterResource(R.drawable.placeholder),
                contentDescription = stringResource(R.string.description),
                contentScale = ContentScale.Crop,
                //modifier = Modifier.clip(CircleShape)
                modifier = Modifier.clip(RectangleShape)
            )

            /*
            capturedImageUri?.let {
                Image(
                    modifier = Modifier
                        .clip(RectangleShape)
                        .fillMaxSize(),
                    painter = rememberImagePainter(it),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                )
            }
            */
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Scaffold(
    topBarTitle: String,
    onFabClick: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(topBarTitle)
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier
                    .height(50.dp),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = Bony,//MaterialTheme.colorScheme.primary,
            ) {
                Row (verticalAlignment = Alignment.CenterVertically){
                    Column { Icon(Icons.Default.WarningAmber, contentDescription = "Warn") }
                    Spacer(Modifier.size(10.dp))
                    Column {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            text = "Created by Mirko Lehn",
                        )
                    }
                    Spacer(Modifier.size(10.dp))
                    Column { Icon(Icons.Default.WarningAmber, contentDescription = "Warn") }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onFabClick,
                shape = RoundedCornerShape(50),
                //containerColor = Color.Black,
                //modifier = Modifier
                //    .border(1.dp, Color.White)
            ) {
                //camera_enhance
                Icon(Icons.Default.AddAPhoto, contentDescription = "AddPhoto")
                //Icon(Icons.Rounded.Menu, contentDescription = "Localized description")
            }
        },

    ) { paddingValues ->
        content(Modifier.padding(paddingValues))
    }
}

fun Context.createImageFile(): File {
    // Create an image file name
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val imageFileName = "JPEG_" + timeStamp + "_"
    val image = File.createTempFile(
        imageFileName, /* prefix */
        ".jpg", /* suffix */
        externalCacheDir      /* directory */
    )
    return image
}