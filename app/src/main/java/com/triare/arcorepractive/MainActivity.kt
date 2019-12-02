package com.triare.arcorepractive

import android.animation.ObjectAnimator
import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.LinearInterpolator
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.QuaternionEvaluator
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MainActivity : AppCompatActivity() {

    private var solModel: ModelRenderable? = null
    private var mercurioModel: ModelRenderable? = null
    private var venusModel: ModelRenderable? = null
    private var earthModel: ModelRenderable? = null
    private var marsModel: ModelRenderable? = null
    private var jupiterModel: ModelRenderable? = null
    private var saturnModel: ModelRenderable? = null
    private var uranusModel: ModelRenderable? = null
    private var neptuneModel: ModelRenderable? = null

    private val arScene: ArFragment by lazy {
        supportFragmentManager.findFragmentById(R.id.ar_scene) as ArFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arScene.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
            val anchor =
                hitResult.createAnchor()
            val anchorNode = AnchorNode(anchor)
            anchorNode.setParent(arScene.arSceneView.scene)
            anchorNode.localPosition = Vector3(0f, -5f, 0f)
            CoroutineScope(Dispatchers.Main).launch {
                val node = async { createSollarSystem() }.await()
                anchorNode.addChild(node)
            }
        }

    }

    private suspend fun createSollarSystem(): Node {
        CoroutineScope(Dispatchers.Main).async {
            solModel = createPlanet(this@MainActivity, "Sol.sfb")
            mercurioModel = createPlanet(this@MainActivity, "Mercury.sfb")
            venusModel = createPlanet(this@MainActivity, "Venus.sfb")
            earthModel = createPlanet(this@MainActivity, "Earth.sfb")
            marsModel = createPlanet(this@MainActivity, "Mars.sfb")
            jupiterModel = createPlanet(this@MainActivity, "Jupiter.sfb")
            saturnModel = createPlanet(this@MainActivity, "Saturn.sfb")
            uranusModel = createPlanet(this@MainActivity, "Uranus.sfb")
            neptuneModel = createPlanet(this@MainActivity, "Neptune.sfb")
        }.await()
        return createPlanets()
    }

    private fun createPlanets(): Node {
        val sunBase = Node()
        sunBase.localScale = Vector3(0.5f, 0.5f, 0.5f)
        sunBase.localPosition = Vector3(0f, 1f, 0f)

        createPlanetNode(sunBase, solModel, Vector3(0f, 0f, 0f), Vector3(1f, 1f, 1f), null)

        createPlanetNode(sunBase, mercurioModel, Vector3(0.5f, 0f, 0f), Vector3(0.2f, 0.2f, 0.2f), 25000)

        createPlanetNode(sunBase, venusModel, Vector3(0.8f, 0f, 0.5f), Vector3(0.3f, 0.3f, 0.3f), 20000)

        createPlanetNode(sunBase, earthModel, Vector3(1.2f, 0f, 0f), Vector3(0.4f, 0.4f, 0.4f), 15000)

        createPlanetNode(sunBase, marsModel, Vector3(1.6f, 0f, 0f), Vector3(0.3f, 0.3f, 0.3f), 30000)

        createPlanetNode(sunBase, jupiterModel, Vector3(2.1f, 0f, 0f), Vector3(0.5f, 0.5f, 0.5f), 12000)

        createPlanetNode(sunBase, saturnModel, Vector3(2.8f, 0f, 0f), Vector3(0.5f, 0.5f, 0.5f), 27000)

        createPlanetNode(sunBase, uranusModel, Vector3(3.8f, 0f, 0f), Vector3(0.9f, 0.9f, 0.9f), 23000)

        createPlanetNode(sunBase, neptuneModel, Vector3(4.6f, 0f, 0f), Vector3(0.4f, 0.4f, 0.4f), 10000)

        return sunBase
    }

    /*private fun getAnimationDuration(): Long {
        return (100 * 360 / (1 * 1f)).toLong()
    }*/

    private suspend fun createPlanet(
        context: Context,
        uri: String
    ): ModelRenderable = suspendCoroutine { continuation ->
        ModelRenderable.builder()
            .setSource(context, Uri.parse(uri))
            .build()
            .thenAccept {
                continuation.resume(it)
            }
            .exceptionally {
                continuation.resumeWithException(it)
                return@exceptionally null
            }
    }

    private fun createPlanetNode(parent: Node,
                               renderable: ModelRenderable?,
                               localPosition: Vector3,
                               localScale: Vector3,
                               aniSpeed: Long? = null
  ) {

    val simpleAniNode = Node()
    simpleAniNode.setParent(parent)

    val sun = Node()
    sun.setParent(simpleAniNode)
    sun.renderable = renderable
    sun.localScale = localScale
    sun.localPosition = localPosition

    if (aniSpeed != null) {
      createAnimator(true, 0f).apply {
        target = simpleAniNode
        duration = aniSpeed
      }.start()
    }

  }


  private fun createAnimator(clockwise: Boolean, axisTiltDeg: Float): ObjectAnimator {
    // Node's setLocalRotation method accepts Quaternions as parameters.
    // First, set up orientations that will animate a circle.
    val orientations = arrayOfNulls<Quaternion>(4)
    // Rotation to apply first, to tilt its axis.
    val baseOrientation = Quaternion.axisAngle(Vector3(1.0f, 0f, 0.0f), axisTiltDeg)
    for (i in orientations.indices) {
      var angle = (i * 360 / (orientations.size - 1)).toFloat()
      if (clockwise) {
        angle = 360 - angle
      }
      val orientation = Quaternion.axisAngle(Vector3(0.0f, 1.0f, 0.0f), angle)
      orientations[i] = Quaternion.multiply(baseOrientation, orientation)
    }

    val orbitAnimation = ObjectAnimator()
    // Cast to Object[] to make sure the varargs overload is called.
    orbitAnimation.setObjectValues(*orientations as Array<Any>)

    // Next, give it the localRotation property.
    orbitAnimation.setPropertyName("localRotation")

    // Use Sceneform's QuaternionEvaluator.
    orbitAnimation.setEvaluator(QuaternionEvaluator())

    //  Allow orbitAnimation to repeat forever
    orbitAnimation.repeatCount = ObjectAnimator.INFINITE
    orbitAnimation.repeatMode = ObjectAnimator.RESTART
    orbitAnimation.interpolator = LinearInterpolator()
    orbitAnimation.setAutoCancel(true)

    return orbitAnimation
  }

}
