package info.staticfree.supergenpass.drawable

import android.graphics.*
import kotlin.jvm.JvmOverloads
import android.graphics.drawable.Drawable
import java.security.MessageDigest

/*
 Visual Hash (SHA-1)
 Copyright (C) 2009-2021  Steve Pomeroy <steve@staticfree.info>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.

 */

/**
 *
 * This displays a visual representation of the output of a hash function. Bits in the hash
 * output are mapped directly to colored shapes and their positions.
 *
 * This is intended to allow rapid visual identification of a known input based by using spatial
 * memory. For example, by hashing a password and displaying the output to the user as they type it,
 * they can learn to identify when they have typed the password successfully by recognizing their
 * password's distinct shapes, colors and arrangements.
 *
 * Make sure that any hash function you specify has an even number of bytes in the output,
 * as each shape requires 2 bytes of data.
 *
 * This will draw n/2 shapes, where n is the number of bytes of output from the hash function.
 * These shapes will possibly overlap and possibly be the same color as the background, so one
 * shouldn't rely on a certain number of shapes to be visible at any given time.
 *
 * @param hashFunction the name of the hash function, eg. "SHA-1"
 * @author Steve Pomeroy
 */
class VisualHash @JvmOverloads constructor(hashFunction: String = "SHA-1") : Drawable() {
    private val messageDigest: MessageDigest = MessageDigest.getInstance(hashFunction)
    private var hash: ByteArray? = null
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        strokeWidth = 0f
    }
    private var scaleX = 0f
    private var scaleY = 0f

    /**
     * Sets the data and digests it into the visual hash.
     *
     * @param input the data to be hashed. This value is discarded immediately after computing the
     * hash.
     */
    fun setData(input: ByteArray) {
        hash = if (input.isEmpty()) {
            null
        } else {
            messageDigest.update(input)
            messageDigest.digest()
        }
        invalidateSelf()
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        val width = bounds.width()
        val height = bounds.height()
        scaleX = width / PRESCALE_WIDTH.toFloat()
        scaleY = height / PRESCALE_HEIGHT.toFloat()
    }

    override fun draw(canvas: Canvas) {
        canvas.scale(scaleX, scaleY)

        hash?.let {
            // go through all the bytes in the hash and draw them as shapes.
            var offset = 0
            while (offset < it.size) {
                val dat = 0xff and it[offset].toInt() or (0xff00 and (it[offset + 1].toInt() shl 8))
                val type = dat and TYPE_MASK
                val x = dat and X_MASK shr X_OFFSET
                val y = dat and Y_MASK shr Y_OFFSET

                // TODO use this bit for something
                // final int a = (dat & A_MASK) >> A_OFFSET;
                p.setARGB(
                    SHAPE_ALPHA, scaleInt(R_MAX, dat and R_MASK shr R_OFFSET, 255),
                    scaleInt(G_MAX, dat and G_MASK shr G_OFFSET, 255),
                    scaleInt(B_MAX, dat and B_MASK shr B_OFFSET, 255)
                )
                val xCenterScaled = ORIGIN_OFFSET + scale(X_MAX, x, PRESCALE_CENTER_WIDTH)
                val yCenterScaled = ORIGIN_OFFSET + scale(Y_MAX, y, PRESCALE_CENTER_WIDTH)
                canvas.save()
                canvas.translate(xCenterScaled, yCenterScaled)
                when (type) {
                    TYPE_STAR -> canvas.drawPath(STAR, p)
                    TYPE_CIRCLE -> canvas.drawCircle(0f, 0f, RADIUS.toFloat(), p)
                    TYPE_TRIANGLE -> canvas.drawPath(TRIANGLE, p)
                    TYPE_SQUARE -> canvas.drawRect(
                        -RADIUS.toFloat(),
                        -RADIUS.toFloat(),
                        RADIUS.toFloat(),
                        RADIUS.toFloat(),
                        p
                    )
                    TYPE_PLUS -> canvas.drawPath(PLUS, p)
                    TYPE_X -> canvas.drawPath(X, p)
                    TYPE_DIAMOND -> canvas.drawPath(DIAMOND, p)
                    TYPE_SMALL_CIRCLE -> canvas.drawCircle(0f, 0f, (RADIUS / 2).toFloat(), p)
                }
                canvas.restore()
                offset += BYTES_PER_SHAPE
            }
        }
    }

    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }

    override fun setAlpha(alpha: Int) {
        // do nothing
    }

    override fun setColorFilter(arg0: ColorFilter?) {
        // do nothing
    }

    override fun getIntrinsicWidth(): Int {
        return PRESCALE_WIDTH
    }

    override fun getIntrinsicHeight(): Int {
        return PRESCALE_HEIGHT
    }

    companion object {
        private const val TYPE_CIRCLE = 0x00
        private const val TYPE_SQUARE = 0x01
        private const val TYPE_STAR = 0x02
        private const val TYPE_TRIANGLE = 0x03
        private const val TYPE_PLUS = 0x04
        private const val TYPE_X = 0x05
        private const val TYPE_DIAMOND = 0x06
        private const val TYPE_SMALL_CIRCLE = 0x07

        // the below defines the offsets to pull the bits of the hash out into
        // visual characteristics.
        // how many bytes per shape. The mapping below would need to be adjusted if this is
        // changed.
        private const val BYTES_PER_SHAPE = 2

        // the shape type
        private const val TYPE_MAX = 0x7
        private const val TYPE_MASK = TYPE_MAX

        // this creates a grid of 4x4 potential shapes.
        private const val X_OFFSET = 3
        private const val X_MAX = 0x7
        private const val X_MASK = X_MAX shl X_OFFSET
        private const val Y_OFFSET = 6
        private const val Y_MAX = 0x7
        private const val Y_MASK = Y_MAX shl Y_OFFSET

        // There are 64 possible colors
        private const val R_OFFSET = 9
        private const val R_MAX = 0x3
        private const val R_MASK = R_MAX shl R_OFFSET
        private const val G_OFFSET = 11
        private const val G_MAX = 0x3
        private const val G_MASK = G_MAX shl G_OFFSET
        private const val B_OFFSET = 13
        private const val B_MAX = 0x3
        private const val B_MASK = B_MAX shl B_OFFSET
        // one extra bit remain
        // ,A_OFFSET = 15, A_MAX = 0x1, A_MASK = A_MAX << A_OFFSET
        /**
         * The spacing between the shapes. Units are pre-scale pixels.
         */
        private const val SPACING = 2
        private const val SHAPE_ALPHA = 255

        /**
         * This constant is based on the static shape definitions.
         */
        private const val RADIUS = 8

        /**
         * The amount to move 0,0 so that drawing a circle with the given radius will be visible.
         */
        private const val ORIGIN_OFFSET = RADIUS + SPACING / 2

        /**
         * pixel width; centers are 3 shapes wide
         */
        private const val PRESCALE_CENTER_WIDTH = (RADIUS * 2 + SPACING) * 3

        /**
         * The size of the rendered area before it's been scaled to fit the drawable's bounds. Width is
         * 4 shapes wide.
         */
        private const val PRESCALE_WIDTH = (RADIUS * 2 + SPACING) * 4
        private const val PRESCALE_HEIGHT = (RADIUS * 2 + SPACING) * 4

        private val TRIANGLE = Path().apply {
            moveTo(-RADIUS.toFloat(), RADIUS.toFloat())
            lineTo(RADIUS.toFloat(), RADIUS.toFloat()) // _
            lineTo(0f, -RADIUS.toFloat())
            lineTo(-RADIUS.toFloat(), RADIUS.toFloat())
        }

        private val STAR = Path().apply {
            // star is based on radius size 8
            //
            // this was drawn in Inkscape and converted to this using
            // the python script svg2java.py included in /extras
            moveTo(0.000000f, -8.475681f)
            lineTo(1.893601f, -2.597389f)
            lineTo(8.069343f, -2.612960f)
            lineTo(3.063910f, 1.004453f)
            lineTo(4.987128f, 6.873122f)
            lineTo(0.000000f, 3.230514f)
            lineTo(-4.987129f, 6.873122f)
            rLineTo(1.923218f, -5.868669f)
            rLineTo(-5.005433f, -3.617414f)
            rLineTo(6.175743f, 0.015571f)
            lineTo(0.000000f, -8.475681f)
        }

        private val PLUS = Path().apply {
            rMoveTo(2.084458f, -2.117061f)
            rLineTo(5.865234f, 0.000000f)
            rLineTo(0.000000f, 4.296875f)
            rLineTo(-5.865234f, 0.000000f)
            rLineTo(0.000000f, 5.865234f)
            rLineTo(-4.296875f, 0.000000f)
            rLineTo(0.000000f, -5.865234f)
            rLineTo(-5.865234f, 0.000000f)
            rLineTo(0.000000f, -4.296875f)
            rLineTo(5.865234f, 0.000000f)
            rLineTo(0.000000f, -5.875977f)
            rLineTo(4.296875f, 0.000000f)
            rLineTo(0.000000f, 5.875977f)
        }

        private val X = Path().apply {
            moveTo(3.723963f, 0.060475f)
            lineTo(8.083338f, 4.419850f)
            lineTo(4.438807f, 8.064382f)
            lineTo(0.079432f, 3.705007f)
            lineTo(-4.279943f, 8.064382f)
            lineTo(-7.924475f, 4.419850f)
            rLineTo(4.359375f, -4.359375f)
            rLineTo(-4.359375f, -4.359375f)
            rLineTo(3.644531f, -3.644531f)
            rLineTo(4.359375f, 4.359375f)
            rLineTo(4.359375f, -4.371094f)
            rLineTo(3.644531f, 3.644531f)
            rLineTo(-4.359375f, 4.371094f)
        }

        private val DIAMOND = Path().apply {
            moveTo(0f, -RADIUS.toFloat())
            lineTo(RADIUS.toFloat(), 0f)
            lineTo(0f, RADIUS.toFloat())
            lineTo(-RADIUS.toFloat(), 0f)
            lineTo(0f, -RADIUS.toFloat())
        }

        /**
         * Scale an int linearly, starting at zero.
         *
         * @param valueMax the maximum input value
         * @param value the value to scale
         * @param max the maximum output value
         * @return the scaled value as an int
         */
        private fun scaleInt(valueMax: Int, value: Int, max: Int): Int {
            return (value / valueMax.toFloat() * max).toInt()
        }

        /**
         * Scale an int linearly, starting at zero.
         *
         * @param valueMax the maximum input value
         * @param value the value to scale
         * @param max the maximum output value
         * @return the scaled value as a float
         */
        private fun scale(valueMax: Int, value: Int, max: Int): Float {
            return value / valueMax.toFloat() * max
        }
    }
}