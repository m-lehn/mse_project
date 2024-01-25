package com.example.mse_project

//import kotlinx.coroutines.flow.internal.NopCollector
//import android.media.ExifInterface
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.sharp.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.example.mse_project.TensorFLowHelper.imageSize
import com.example.mse_project.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

@Composable
fun WarningPopupDialog(onDismissRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* Dismiss dialog when user touches outside */ },
        title = {
            Text(text = "Important Warning")
        },
        text = {
            Column {
                Text("Please be aware that this app uses a neural network to classify mushrooms. However, neural networks cannot guarantee absolutely correct results.")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Always double-check with a professional or reliable source before making any decisions based on the app's classifications.")
            }
        },
        confirmButton = {
            Button(
                onClick = onDismissRequest
            ) {
                Text("Understand")
            }
        },
        icon = {
            Icon(Icons.Default.WarningAmber, contentDescription = "Warn")
        }
    )
}

@Composable
fun AdvertismentPopup(
    patreonImageId: Int,
    isPopupVisible: Boolean,
    onCloseClick: () -> Unit
) {
    if (isPopupVisible) {
        Dialog(onDismissRequest = onCloseClick) {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
            ) {
                Image(
                    painter = painterResource(id = patreonImageId),
                    contentDescription = "Patreon Image"
                )
                IconButton(
                    onClick = onCloseClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .background(
                            color = Color.Transparent,
                            shape = RectangleShape
                        )
                        .border(1.dp, Color.Gray, RectangleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}


//@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreen() {

    // Initial Warning
    var showWarning by rememberSaveable { mutableStateOf(true) }
    if (showWarning) {
        WarningPopupDialog(onDismissRequest = { showWarning = false })
    }

    // Advertisment
    var untillAdvertisment by rememberSaveable { mutableStateOf(3) }
    var showPatreonPopup by rememberSaveable { mutableStateOf(false) }
    AdvertismentPopup(
        patreonImageId = R.drawable.patreon, // Replace with your image resource ID
        isPopupVisible = showPatreonPopup,
        onCloseClick = { showPatreonPopup = false }
    )

    val context = LocalContext.current
    var bitmap by rememberSaveable { mutableStateOf<Bitmap?>(null) }

    // Prepare a file and URI for the captured image
    val file = rememberSaveable { context.createImageFile() }
    val uri = rememberSaveable { FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", file) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            untillAdvertisment--
            if (untillAdvertisment == 0) {
                untillAdvertisment = 3
                showPatreonPopup = true
            }
            bitmap = cropSquare(rotateBitmapIfNeeded(file.absolutePath, BitmapFactory.decodeFile(file.absolutePath)))
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBarTitle = "Shrooming - The App",
        onFabClick = {
            bitmap?.recycle()
            bitmap = null
            when (PackageManager.PERMISSION_GRANTED) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                    cameraLauncher.launch(uri)
                }
                else -> permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    ) {

    }
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val size = min(screenWidth, screenHeight) - 64.dp // - ?.dp Padding
    Box(
        modifier = Modifier
            .size(size)
            .clip(RectangleShape)
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedCard(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            border = BorderStroke(1.dp, Color.White),
            modifier = Modifier
                .size(size)
                .clip(RectangleShape)
                //.padding(15.dp)
                //.padding(15.dp)
                .align(Alignment.Center)
        ) {
            if (bitmap != null) {
                bitmap?.let { ImageArea(it) }
            } else {
                // Display placeholder image from assets
                Image(
                    painter = painterResource(id = R.drawable.placeholder),
                    contentDescription = "Placeholder Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
    bitmap?.let{ClassifiedResult(it)}
}


fun rotateBitmapIfNeeded(photoPath: String, bitmap: Bitmap): Bitmap {
    val ei = ExifInterface(photoPath)

    return when (ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
        ExifInterface.ORIENTATION_NORMAL -> bitmap
        else -> bitmap
    }
}

private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(angle) }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

private fun cropSquare(bitmap: Bitmap): Bitmap {
    val size = min(bitmap.width, bitmap.height)
    val startX = (bitmap.width - size) / 2
    val startY = (bitmap.height - size) / 2
    return Bitmap.createBitmap(bitmap, startX, startY, size, size)
}

private fun scaleBitmap(bitmap: Bitmap): Bitmap {
    return Bitmap.createScaledBitmap(bitmap, 224, 224, true)
}

@Composable
fun ClassifiedResult(
    bitmap: Bitmap
){
    bitmap.let {
        Log.d("classifiedText", "classifiedText")
        val scaledBitmap = Bitmap.createScaledBitmap(cropSquare(it), imageSize, imageSize, false)
        TensorFLowHelper.classifyImage(scaledBitmap) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Image is classified as:",
                    style = TextStyle(
                        fontSize = 24.sp,
                        color = Color.White,
                        shadow = Shadow(
                            color = Color.Black, offset = Offset(5.0f, 5.0f), blurRadius = 3f
                        )
                    )
                )
                Text(
                    text = it,
                    color = Color.White,
                    fontSize = 35.sp,
                    style = TextStyle(
                        fontSize = 35.sp,
                        shadow = Shadow(
                            color = Color.Black, offset = Offset(5.0f, 5.0f), blurRadius = 3f
                        )
                    )
                )
            }
        }
    }
}

@Composable
fun ImageArea(
    bitmap: Bitmap
) {
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Photo Display Area",
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun NewsTicker(textList: List<String>) {
    val infiniteTransition = rememberInfiniteTransition(label = "infiniteTransition")
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val translateAnim = infiniteTransition.animateFloat(
        initialValue = 1.2f, // Start position
        targetValue = -1.2f, // End position
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "translateAnim"
    )

    val totalContentWidth = remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    totalContentWidth.value = coordinates.size.width.toFloat()
                }
                .graphicsLayer {
                    translationX = translateAnim.value * totalContentWidth.value
                }
                .wrapContentWidth(unbounded = true)
                //.width(1000.dp)
        ) {
            textList.forEach { text ->
                Column {
                    Text(text = text, maxLines = 1, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    //Icon(Icons.Default.WarningAmber, contentDescription = "Warn")
                    Text(text = " +++ ", maxLines = 1, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
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
                    //Column { Icon(Icons.Default.WarningAmber, contentDescription = "Warn") }
//                    Spacer(Modifier.size(10.dp))
//                    Column {
//                        Text(
//                            modifier = Modifier
//                                .fillMaxWidth(),
//                            textAlign = TextAlign.Center,
//                            text = "Created by Mirko Lehn",
//                        )
//                    }
//                    Spacer(Modifier.size(10.dp))
                    val message1 = "created by Mirko Lehn"
                    val message2 = "correctness not guaranteed"
                    val message3 = "consider supporting me on patreon"
                    val message4 = "eat more mushrooms"
                    val message5 = "consider buying me some coffee"
                    val tickerTextList = listOf(message1, message2, message3, message4, message5)
                    NewsTicker(textList = tickerTextList)
                    //Column { Icon(Icons.Default.WarningAmber, contentDescription = "Warn") }
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
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val imageFileName = "JPEG_$timeStamp"

    // Create and return a temporary file
    return File.createTempFile(
        imageFileName, /* prefix */
        ".jpg", /* suffix */
        externalCacheDir /* directory */
    )
}
