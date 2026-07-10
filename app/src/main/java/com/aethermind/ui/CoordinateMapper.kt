package com.aethermind.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

/**
 * CoordinateMapper - แปลงพิกัดระหว่าง Engine กับหน้าจอ
 * 
 * Design Principles:
 * - Device Agnostic: ทำงานได้ทุก resolution และ aspect ratio
 * - Zero-allocation: ใช้ primitive operations ไม่สร้าง object ใหม่
 * - Performance: การคูณ float กินทรัพยากรน้อยมาก
 * 
 * Coordinate Systems:
 * - Engine: Normalized coordinates (0.0 - 1.0) จาก C++ VisionProcessor
 * - Screen: Pixel coordinates (0 - width, 0 - height) บนหน้าจอจริง
 */
object CoordinateMapper {
    
    /**
     * แปลงพิกัดจาก Engine (0.0-1.0) เป็นพิกัดหน้าจอ (Pixels)
     * 
     * @param normX ค่า X จาก C++ (0.0-1.0)
     * @param normY ค่า Y จาก C++ (0.0-1.0)
     * @param screenSize ขนาดหน้าจอปัจจุบัน (IntSize)
     * @return Offset ในพิกัด pixel
     */
    fun mapToScreen(normX: Float, normY: Float, screenSize: IntSize): Offset {
        // Inline multiplication - fastest path
        val screenX = normX * screenSize.width
        val screenY = normY * screenSize.height
        return Offset(screenX, screenY)
    }
    
    /**
     * แปลงพิกัดจาก Engine เป็น Pixel โดยตรง (Overload)
     * 
     * @param normX ค่า X จาก C++ (0.0-1.0)
     * @param normY ค่า Y จาก C++ (0.0-1.0)
     * @param screenWidth ความกว้างหน้าจอ (pixels)
     * @param screenHeight ความสูงหน้าจอ (pixels)
     * @return Offset ในพิกัด pixel
     */
    fun mapToScreen(normX: Float, normY: Float, screenWidth: Int, screenHeight: Int): Offset {
        return Offset(normX * screenWidth, normY * screenHeight)
    }
    
    /**
     * แปลงพิกัดจาก Pixel เป็น Normalized (Engine coordinates)
     * 
     * ใช้สำหรับกรณีที่ต้องการแปลงกลับ
     * 
     * @param screenX ค่า X บนหน้าจอ (pixels)
     * @param screenY ค่า Y บนหน้าจอ (pixels)
     * @param screenSize ขนาดหน้าจอปัจจุบัน
     * @return Pair (normX, normY) ในรูปแบบ 0.0-1.0
     */
    fun mapToEngine(screenX: Float, screenY: Float, screenSize: IntSize): Pair<Float, Float> {
        val normX = screenX / screenSize.width
        val normY = screenY / screenSize.height
        return Pair(normX, normY)
    }
    
    /**
     * ตรวจสอบว่าพิกัดอยู่ในขอบเขตหน้าจอหรือไม่
     * 
     * @param normX ค่า X จาก C++ (0.0-1.0)
     * @param normY ค่า Y จาก C++ (0.0-1.0)
     * @return true ถ้าอยู่ในขอบเขต [0, 1]
     */
    fun isVisible(normX: Float, normY: Float): Boolean {
        return normX >= 0f && normX <= 1f && normY >= 0f && normY <= 1f
    }
    
    /**
     * ตรวจสอบว่าพิกัดอยู่ในขอบเขตด้วย Tolerance
     * 
     * @param normX ค่า X จาก C++ (0.0-1.0)
     * @param normY ค่า Y จาก C++ (0.0-1.0)
     * @param margin ค่า tolerance (0.0-0.5)
     * @return true ถ้าอยู่ในขอบเขต [margin, 1-margin]
     */
    fun isVisible(normX: Float, normY: Float, margin: Float): Boolean {
        return normX >= margin && normX <= (1f - margin) &&
               normY >= margin && normY <= (1f - margin)
    }
    
    /**
     * Scale factor สำหรับการคำนวณขนาด (radius, stroke)
     * 
     * ปรับขนาดตามความละเอียดหน้าจอ
     * 
     * @param baseRadius ขนาดพื้นฐาน (base on 1080p screen)
     * @param screenSize ขนาดหน้าจอปัจจุบัน
     * @return ขนาดที่ปรับตาม resolution
     */
    fun scaleRadius(baseRadius: Float, screenSize: IntSize): Float {
        // Base on 1080p (1080 width)
        val referenceWidth = 1080f
        val scale = screenSize.width / referenceWidth
        return baseRadius * scale
    }
    
    /**
     * คำนวณระยะห่างระหว่าง 2 จุดในพิกัด Normalized
     * 
     * @param x1 พิกัด X จุดที่ 1
     * @param y1 พิกัด Y จุดที่ 1
     * @param x2 พิกัด X จุดที่ 2
     * @param y2 พิกัด Y จุดที่ 2
     * @return ระยะห่าง (0.0-1.0)
     */
    fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
    
    /**
     * คำนวณมุมระหว่าง 2 จุด (ในองศา)
     * 
     * @param fromX จุดเริ่มต้น X
     * @param fromY จุดเริ่มต้น Y
     * @param toX จุดปลายทาง X
     * @param toY จุดปลายทาง Y
     * @return มุมในองศา (0-360)
     */
    fun angleBetween(fromX: Float, fromY: Float, toX: Float, toY: Float): Float {
        val dx = toX - fromX
        val dy = toY - fromY
        val radians = kotlin.math.atan2(dy.toDouble(), dx.toDouble())
        var degrees = Math.toDegrees(radians).toFloat()
        if (degrees < 0) degrees += 360f
        return degrees
    }
    
    /**
     * ขยายเส้นวิถีออกไปจากจุดเริ่มต้น
     * 
     * ใช้สำหรับวาดเส้นเล็งให้ยาวขึ้นผ่านจุดเป้าหมาย
     * 
     * @param startX จุดเริ่มต้น X
     * @param startY จุดเริ่มต้น Y
     * @param endX จุดปลายทาง X
     * @param endY จุดปลายทาง Y
     * @param extensionFactor ตัวคูณสำหรับขยาย (เช่น 1.5 = ยาวขึ้น 50%)
     * @return Pair (extendedX, extendedY)
     */
    fun extendLine(
        startX: Float, startY: Float,
        endX: Float, endY: Float,
        extensionFactor: Float
    ): Pair<Float, Float> {
        // Vector from start to end
        val dirX = endX - startX
        val dirY = endY - startY
        
        // Extended end point
        val extendedX = endX + dirX * extensionFactor
        val extendedY = endY + dirY * extensionFactor
        
        return Pair(extendedX, extendedY)
    }
    
    /**
     * ตรวจสอบว่าหน้าจอเป็นแนวตั้งหรือแนวนอน
     * 
     * @param screenSize ขนาดหน้าจอ
     * @return true ถ้าเป็นแนวตั้ง (height > width)
     */
    fun isPortrait(screenSize: IntSize): Boolean {
        return screenSize.height > screenSize.width
    }
    
    /**
     * คำนวณ aspect ratio ของหน้าจอ
     * 
     * @param screenSize ขนาดหน้าจอ
     * @return aspect ratio (width/height)
     */
    fun aspectRatio(screenSize: IntSize): Float {
        return screenSize.width.toFloat() / screenSize.height.toFloat()
    }
}

/**
 * Data class สำหรับเก็บข้อมูล Ball Position จาก Vision
 * 
 * ใช้ร่วมกับ CoordinateMapper เพื่อวาดบน Canvas
 */
data class BallPosition(
    val id: Int,
    val normX: Float,  // Normalized 0.0-1.0
    val normY: Float,  // Normalized 0.0-1.0
    val isCue: Boolean = false,
    val confidence: Float = 1.0f
) {
    /**
     * แปลงเป็น Offset บนหน้าจอ
     */
    fun toScreenOffset(screenSize: IntSize): Offset {
        return CoordinateMapper.mapToScreen(normX, normY, screenSize)
    }
    
    /**
     * ตรวจสอบว่ามองเห็นได้บนหน้าจอ
     */
    fun isVisible(): Boolean {
        return CoordinateMapper.isVisible(normX, normY)
    }
}

/**
 * Data class สำหรับเส้นวิถี (Trajectory Line)
 */
data class TrajectoryLine(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float
) {
    /**
     * แปลงเป็น Pair ของ Offset บนหน้าจอ
     */
    fun toScreenOffsets(screenSize: IntSize): Pair<Offset, Offset> {
        return Pair(
            CoordinateMapper.mapToScreen(startX, startY, screenSize),
            CoordinateMapper.mapToScreen(endX, endY, screenSize)
        )
    }
    
    /**
     * สร้างเส้นวิถีจากลูกขาวไปยังลูกเป้าหมาย
     */
    companion object {
        fun fromCueToTarget(cue: BallPosition, target: BallPosition): TrajectoryLine {
            return TrajectoryLine(
                startX = cue.normX,
                startY = cue.normY,
                endX = target.normX,
                endY = target.normY
            )
        }
        
        /**
         * สร้างเส้นวิถีแบบขยายออกไป (สำหรับวาดเส้นเล็งยาว)
         */
        fun fromCueToTargetExtended(
            cue: BallPosition, 
            target: BallPosition,
            extensionFactor: Float = 0.3f
        ): TrajectoryLine {
            val (endX, endY) = CoordinateMapper.extendLine(
                cue.normX, cue.normY,
                target.normX, target.normY,
                extensionFactor
            )
            return TrajectoryLine(
                startX = cue.normX,
                startY = cue.normY,
                endX = endX,
                endY = endY
            )
        }
    }
}