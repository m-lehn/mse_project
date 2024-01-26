package com.example.mse_project

//import kotlinx.coroutines.flow.internal.NopCollector
//import android.media.ExifInterface
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
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
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.sharp.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
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
    onCloseClick: () -> Unit,
    onBoxClick: () -> Unit
) {
    if (isPopupVisible) {
        Dialog(onDismissRequest = {}) {
            Box(
                modifier = Modifier
                    .clickable(onClick = onBoxClick)
                    .wrapContentSize()
                    .background(Color.Transparent, shape = RoundedCornerShape(8.dp))
            ) {
                Image(
                    painter = painterResource(id = patreonImageId),
                    contentDescription = "Patreon Image"
                )
                IconButton(
                    onClick = onCloseClick, // Only closes the dialog
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

fun openWebPage(context: Context, url: String) {
    //Log.d("AdvertismentPopup", "Attempting to open web page: $url")
    val webpage: Uri = Uri.parse(url)
    val intent = Intent(Intent.ACTION_VIEW, webpage)
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        //Log.e("AdvertismentPopup", "No Intent available to handle action")
    }
}

@Composable
fun MyScreen() {

    val context = LocalContext.current
    var bitmap by rememberSaveable { mutableStateOf<Bitmap?>(null) }

    // Initial Warning
    var showWarning by rememberSaveable { mutableStateOf(true) }
    if (showWarning) {
        WarningPopupDialog(onDismissRequest = { showWarning = false })
    }

    // Advertisment
    val webpageUrl = "https://www.patreon.com/"
    var untillAdvertisment by rememberSaveable { mutableStateOf(3) }
    var showPatreonPopup by rememberSaveable { mutableStateOf(false) }
    AdvertismentPopup(
        patreonImageId = R.drawable.patreon,
        isPopupVisible = showPatreonPopup,
        onCloseClick = { showPatreonPopup = false },
        onBoxClick = { openWebPage(context, webpageUrl) }
    )

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
    val size = min(screenWidth, screenHeight) - 100.dp // - ?.dp Padding
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
fun ClassifiedResult(bitmap: Bitmap) {
    val context = LocalContext.current
    val (showPopup, setShowPopup) = remember { mutableStateOf(false) }
    var classificationResult by remember { mutableStateOf("") }
    var infoText by remember { mutableStateOf("") }

    bitmap.let {
        Log.d("classifiedText", "classifiedText")
        val scaledBitmap = Bitmap.createScaledBitmap(cropSquare(it), imageSize, imageSize, false)
        TensorFLowHelper.classifyImage(scaledBitmap) { result ->
            classificationResult = result
            infoText = getInfoTextForClassification(result)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Image is classified as:",
            modifier = Modifier.clickable { setShowPopup(true) },
            style = TextStyle(
                fontSize = 24.sp,
                color = Color.White,
                shadow = Shadow(
                    color = Color.Black, offset = Offset(5.0f, 5.0f), blurRadius = 3f
                )
            )
        )
        Text(
            text = classificationResult,
            color = Color.White,
            fontSize = 35.sp,
            modifier = Modifier.clickable { setShowPopup(true) },
            style = TextStyle(
                fontSize = 35.sp,
                shadow = Shadow(
                    color = Color.Black, offset = Offset(5.0f, 5.0f), blurRadius = 3f
                )
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        Icon(Icons.Default.Info, modifier = Modifier.clickable { setShowPopup(true) }, contentDescription = "MoreInformation")
        Text(
            text = "info",
            modifier = Modifier.clickable { setShowPopup(true) },
            style = TextStyle(
                fontSize = 16.sp,
                color = Color.White,
                shadow = Shadow(
                    color = Color.Black, offset = Offset(4.0f, 4.0f), blurRadius = 3f
                )
            )
        )
    }

    if (showPopup) {
        MushroomInfoDialog(
            mushroomClass = classificationResult,
            infoText = infoText,
            onDismiss = { setShowPopup(false) }
        )
    }
}

@Composable
fun MushroomInfoDialog(
    mushroomClass: String, // Add this parameter for the mushroom class name
    infoText: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = mushroomClass) }, // Use mushroomClass here for the title
        text = { Text(text = infoText) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}



@Composable
fun getInfoTextForClassification(classification: String): String {
    return when (classification) {
        "Agaricus" -> stringResource(R.string.agaricus_description)
        "Amanita" -> stringResource(R.string.amanita_description)
        "Boletus" -> stringResource(R.string.boletus_description)
        "Cortinarius" -> stringResource(R.string.cortinarius_description)
        "Entoloma" -> stringResource(R.string.entoloma_description)
        "Hygrocybe" -> stringResource(R.string.hygrocybe_description)
        "Lactarius" -> stringResource(R.string.lactarius_description)
        "Russula" -> stringResource(R.string.russula_description)
        "Suillus" -> stringResource(R.string.suillus_description)
        else -> "Information not available"
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
        // magic numbers:
        initialValue = 3.63f - (screenWidth.value * 0.0025f), // Start position
        targetValue = -3.63f + (screenWidth.value * 0.0025f), // End position
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = screenWidth.value.toInt()*55, easing = LinearEasing),
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
                    translationX = (translateAnim.value * totalContentWidth.value)
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

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun Scaffold(
//    topBarTitle: String,
//    onFabClick: () -> Unit,
//    content: @Composable (Modifier) -> Unit
//) {
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = {
//                    Row(verticalAlignment = Alignment.CenterVertically) {
//                        Text(topBarTitle)
//                        Spacer(Modifier.width(8.dp)) // Space between text and icon
//                        Icon(Icons.Default.Park, contentDescription = "MoreInformation")
//                    }
//                }
//            )
//        },
//        bottomBar = {
//            BottomAppBar(
//                modifier = Modifier
//                    .height(50.dp),
//                containerColor = MaterialTheme.colorScheme.background,
//                contentColor = Bony,//MaterialTheme.colorScheme.primary,
//            ) {
//                Row (verticalAlignment = Alignment.CenterVertically){
//                    val message1 = "created by Mirko Lehn"
//                    val message2 = "correctness not guaranteed"
//                    val message3 = "consider supporting me on patreon"
//                    val message4 = "eat more mushrooms"
//                    val message5 = "consider buying me some coffee"
//                    val tickerTextList = listOf(message1, message2, message3, message4, message5)
//                    NewsTicker(textList = tickerTextList)
//                }
//            }
//        },
//        floatingActionButton = {
//            OutlinedButton(
//                onClick = onFabClick,
//                border = BorderStroke(2.dp, Color.White),
//                modifier = Modifier.size(width = 70.dp, height = 70.dp),
//                shape = RoundedCornerShape(50),
//            ) {
//                //camera_enhance
//                //Icon(Icons.Default.AddAPhoto, contentDescription = "AddPhoto")
//                Icon(Icons.Default.CameraAlt, contentDescription = "AddPhoto")
//                //Icon(Icons.Rounded.Menu, contentDescription = "Localized description")
//            }
//        },
//
//    ) { paddingValues ->
//        content(Modifier.padding(paddingValues))
//    }
//}

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(topBarTitle)
                        Spacer(Modifier.width(8.dp)) // Space between text and icon
                        Icon(Icons.Default.Park, contentDescription = "MoreInformation")
                    }
                }
            )
        },
        floatingActionButton = {
            OutlinedButton(
                onClick = onFabClick,
                border = BorderStroke(2.dp, Color.White),
                modifier = Modifier.size(width = 70.dp, height = 70.dp),
                shape = RoundedCornerShape(50),
            ) {
                //camera_enhance
                //Icon(Icons.Default.AddAPhoto, contentDescription = "AddPhoto")
                Icon(Icons.Default.CameraAlt, contentDescription = "AddPhoto")
                //Icon(Icons.Rounded.Menu, contentDescription = "Localized description")
            }
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier
                    .height(50.dp),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = Bony,//MaterialTheme.colorScheme.primary,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val message1 = "created by Mirko Lehn"
                    val message2 = "correctness not guaranteed"
                    val message3 = "consider supporting me on patreon"
                    val message4 = "eat more mushrooms"
                    val message5 = "consider buying me some coffee"
                    val tickerTextList = listOf(message1, message2, message3, message4, message5)
                    NewsTicker(textList = tickerTextList)
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Main content
            content(Modifier.padding(paddingValues))

            // Long horizontal mushroom image on top of the BottomAppBar, NICE!
            Image(
                painter = painterResource(id = R.drawable.mushrooms),
                contentDescription = "Mushroom Line",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 50.dp) // Adjust this value based on your BottomAppBar's height
                    .height(50.dp) // Set the desired height
                    .fillMaxWidth(), // Fill the maximum available width
                contentScale = ContentScale.FillHeight // Scale the image to fill the height
            )

        }
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
