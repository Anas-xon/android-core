package com.ailnor.core

import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import kotlin.math.ceil
import kotlin.math.cos


// Created by Anaskhan on 8/27/2021.

class RoundRectDrawable(
    backgroundColor: ColorStateList?,
    radius: Float
) : Drawable() {

    companion object {
        // used to calculate content padding
        private val COS_45 = cos(Math.toRadians(45.0))
        private const val SHADOW_MULTIPLIER = 1.5f

        /*
    * This helper is set by CardView implementations.
    * <p>
    * Prior to API 17, canvas.drawRoundRect is expensive; which is why we need this interface
    * to draw efficient rounded rectangles before 17.
    * */
        var sRoundRectHelper: RoundRectHelper? = null
        fun calculateVerticalPadding(
            maxShadowSize: Float, cornerRadius: Float,
            addPaddingForCorners: Boolean
        ): Float {
            return if (addPaddingForCorners) {
                (maxShadowSize * SHADOW_MULTIPLIER + (1 - COS_45) * cornerRadius).toFloat()
            } else {
                maxShadowSize * SHADOW_MULTIPLIER
            }
        }

        fun calculateHorizontalPadding(
            maxShadowSize: Float, cornerRadius: Float,
            addPaddingForCorners: Boolean
        ): Float {
            return if (addPaddingForCorners) {
                (maxShadowSize + (1 - COS_45) * cornerRadius).toFloat()
            } else {
                maxShadowSize
            }
        }
    }

    interface RoundRectHelper {
        fun drawRoundRect(canvas: Canvas?, bounds: RectF?, cornerRadius: Float, paint: Paint?)
    }

    var mRadius = 0f
    var mPaint: Paint? = null
    var mBoundsF: RectF? = null
    var mBoundsI: Rect? = null
    var mPadding = 0f
    var mInsetForPadding = false
    var mInsetForRadius = true

    var mBackground: ColorStateList? = null
    var mTintFilter: PorterDuffColorFilter? = null
    var mTint: ColorStateList? = null
    var mTintMode: PorterDuff.Mode? = PorterDuff.Mode.SRC_IN

    init {
        mRadius = radius
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        setBackground(backgroundColor)
        mBoundsF = RectF()
        mBoundsI = Rect()
    }


    private fun setBackground(color: ColorStateList?) {
        mBackground = color ?: ColorStateList.valueOf(Color.TRANSPARENT)
        mPaint!!.color = mBackground!!.getColorForState(state, mBackground!!.defaultColor)
    }

    fun setPadding(padding: Float, insetForPadding: Boolean, insetForRadius: Boolean) {
        if (padding == mPadding && mInsetForPadding == insetForPadding && mInsetForRadius == insetForRadius) {
            return
        }
        mPadding = padding
        mInsetForPadding = insetForPadding
        mInsetForRadius = insetForRadius
        updateBounds(null)
        invalidateSelf()
    }

    fun getPadding(): Float {
        return mPadding
    }

    override fun draw(canvas: Canvas) {
        val paint = mPaint
        val clearColorFilter: Boolean
        if (mTintFilter != null && paint!!.colorFilter == null) {
            paint.colorFilter = mTintFilter
            clearColorFilter = true
        } else {
            clearColorFilter = false
        }
        canvas.drawRoundRect(mBoundsF!!, mRadius, mRadius, paint!!)
        if (clearColorFilter) {
            paint.colorFilter = null
        }
    }

    private fun updateBounds(bounds: Rect?) {
        var _bounds = bounds
        if (_bounds == null) {
            _bounds = getBounds()
        }
        mBoundsF!![_bounds.left.toFloat(), _bounds.top.toFloat(), _bounds.right.toFloat()] =
            _bounds.bottom.toFloat()
        mBoundsI!!.set(_bounds)
        if (mInsetForPadding) {
            val vInset = calculateVerticalPadding(
                mPadding,
                mRadius,
                mInsetForRadius
            )
            val hInset = calculateHorizontalPadding(
                mPadding,
                mRadius,
                mInsetForRadius
            )
            mBoundsI!!.inset(
                ceil(hInset.toDouble()).toInt(),
                ceil(vInset.toDouble()).toInt()
            )
            // to make sure they have same bounds.
            mBoundsF!!.set(mBoundsI!!)
        }
    }

    override fun onBoundsChange(bounds: Rect?) {
        super.onBoundsChange(bounds)
        updateBounds(bounds)
    }

    override fun getOutline(outline: Outline) {
        outline.setRoundRect(mBoundsI!!, mRadius)
    }

    fun setRadius(radius: Float) {
        if (radius == mRadius) {
            return
        }
        mRadius = radius
        updateBounds(null)
        invalidateSelf()
    }

    override fun setAlpha(alpha: Int) {
        mPaint!!.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        mPaint!!.colorFilter = cf
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    fun getRadius(): Float {
        return mRadius
    }

    fun setColor(color: ColorStateList?) {
        setBackground(color)
        invalidateSelf()
    }

    fun getColor(): ColorStateList? {
        return mBackground
    }

    override fun setTintList(tint: ColorStateList?) {
        mTint = tint
        mTintFilter = createTintFilter(mTint, mTintMode)
        invalidateSelf()
    }

    override fun setTintMode(tintMode: PorterDuff.Mode?) {
        mTintMode = tintMode
        mTintFilter = createTintFilter(mTint, mTintMode)
        invalidateSelf()
    }

    override fun onStateChange(stateSet: IntArray?): Boolean {
        val newColor = mBackground!!.getColorForState(stateSet, mBackground!!.defaultColor)
        val colorChanged = newColor != mPaint!!.color
        if (colorChanged) {
            mPaint!!.color = newColor
        }
        if (mTint != null && mTintMode != null) {
            mTintFilter = createTintFilter(mTint, mTintMode)
            return true
        }
        return colorChanged
    }

    override fun isStateful(): Boolean {
        return (mTint != null && mTint!!.isStateful
                || mBackground != null && mBackground!!.isStateful || super.isStateful())
    }

    /**
     * Ensures the tint filter is consistent with the current tint color and
     * mode.
     */
    private fun createTintFilter(
        tint: ColorStateList?,
        tintMode: PorterDuff.Mode?
    ): PorterDuffColorFilter? {
        if (tint == null || tintMode == null) {
            return null
        }
        val color = tint.getColorForState(state, Color.TRANSPARENT)
        return PorterDuffColorFilter(color, tintMode)
    }
}