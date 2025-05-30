package com.example.drowzeeapp

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker.FaceLandmarkerOptions // ❌ wrong
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import kotlin.math.sqrt

class FacialLandmarkDetector(context: Context) {

    private val faceLandmarker: FaceLandmarker

    init {
        val options = FaceLandmarkerOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath("face_landmarker.task")
                    .build()
            )
            .setRunningMode(RunningMode.IMAGE)
            .setMinFaceDetectionConfidence(0.5f)
            .setMinFacePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .build()

        faceLandmarker = FaceLandmarker.createFromOptions(context, options)
    }

    fun processImage(bitmap: Bitmap): DetectionResult {
        val mpImage: MPImage = BitmapImageBuilder(bitmap).build()
        val result: FaceLandmarkerResult = faceLandmarker.detect(mpImage)

        if (result.faceLandmarks().isEmpty()) {
            return DetectionResult(false, false, 0f, 0f, "No face detected")
        }

        val landmarks = result.faceLandmarks()[0]

        val leftEyeIndices = listOf(33, 160, 158, 133, 153, 144)
        val rightEyeIndices = listOf(362, 385, 387, 263, 373, 380)
        val mouthIndices = listOf(61, 291, 81, 178, 13, 14, 17, 402, 311, 308)

        val leftEAR = calculateEAR(leftEyeIndices.map { toPoint3D(landmarks[it]) })
        val rightEAR = calculateEAR(rightEyeIndices.map { toPoint3D(landmarks[it]) })
        val ear = (leftEAR + rightEAR) / 2f

        val mar = calculateMAR(mouthIndices.map { toPoint3D(landmarks[it]) })

        val isDrowsy = ear < 0.21f
        val isYawning = mar > 0.6f

        val status = when {
            isDrowsy && isYawning -> "😴 Drowsy and Yawning"
            isDrowsy -> "😴 Drowsy"
            isYawning -> "🗣️ Yawning"
            else -> "✅ Awake"
        }

        return DetectionResult(isDrowsy, isYawning, ear, mar, status)
    }

    private fun toPoint3D(lm: NormalizedLandmark): Point3D {
        return Point3D(lm.x(), lm.y(), lm.z())
    }

    private fun calculateEAR(eyePoints: List<Point3D>): Float {
        val a = distance(eyePoints[1], eyePoints[5])
        val b = distance(eyePoints[2], eyePoints[4])
        val c = distance(eyePoints[0], eyePoints[3])
        return (a + b) / (2.0f * c)
    }

    private fun calculateMAR(mouthPoints: List<Point3D>): Float {
        val a = distance(mouthPoints[2], mouthPoints[6])
        val b = distance(mouthPoints[3], mouthPoints[5])
        val c = distance(mouthPoints[0], mouthPoints[4])
        return (a + b) / (2.0f * c)
    }

    private fun distance(p1: Point3D, p2: Point3D): Float {
        return sqrt((p2.x - p1.x).squared() + (p2.y - p1.y).squared() + (p2.z - p1.z).squared())
    }

    private fun Float.squared(): Float = this * this

    data class Point3D(val x: Float, val y: Float, val z: Float)

    data class DetectionResult(
        val isDrowsy: Boolean,
        val isYawning: Boolean,
        val ear: Float,
        val mar: Float,
        val status: String
    )
}
