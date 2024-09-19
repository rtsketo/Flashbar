package com.andrognito.flashbar

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import android.text.Spanned
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.andrognito.flashbar.Flashbar.Gravity
import com.andrognito.flashbar.Flashbar.Gravity.BOTTOM
import com.andrognito.flashbar.Flashbar.Gravity.TOP
import com.andrognito.flashbar.Flashbar.ProgressPosition.LEFT
import com.andrognito.flashbar.Flashbar.ProgressPosition.RIGHT
import com.andrognito.flashbar.SwipeDismissTouchListener.DismissCallbacks
import com.andrognito.flashbar.anim.FlashAnimIconBuilder
import com.andrognito.flashbar.databinding.FlashBarViewBinding
import com.andrognito.flashbar.util.convertDpToPx
import com.andrognito.flashbar.util.getStatusBarHeightInPx
import com.andrognito.flashbar.view.ShadowView

/**
 * The actual Flashbar view representation that can consist of the title, message, button, icon, etc.
 * Its size is adaptive and depends solely on the amount of content present in it. It always matches
 * the width of the screen.
 *
 * It can either be present at the top or at the bottom of the screen. It will always consume touch
 * events and respond as necessary.
 */
internal class FlashbarView(context: Context) : LinearLayout(context) {

    private val TOP_COMPENSATION_MARGIN = resources.getDimension(R.dimen.fb_top_compensation_margin).toInt()
    private val BOTTOM_COMPENSATION_MARGIN = resources.getDimension(R.dimen.fb_bottom_compensation_margin).toInt()

    private lateinit var parentFlashbarContainer: FlashbarContainerView
    private lateinit var gravity: Gravity

    private var isMarginCompensationApplied: Boolean = false

    // Initialize View Binding
    private val binding: FlashBarViewBinding = FlashBarViewBinding.inflate(LayoutInflater.from(context), this, true)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (!isMarginCompensationApplied) {
            isMarginCompensationApplied = true

            val params = layoutParams as MarginLayoutParams
            when (gravity) {
                TOP -> params.topMargin = -TOP_COMPENSATION_MARGIN
                BOTTOM -> params.bottomMargin = -BOTTOM_COMPENSATION_MARGIN
            }
            requestLayout()
        }
    }

    internal fun init(
        gravity: Gravity,
        castShadow: Boolean,
        shadowStrength: Int
    ) {
        this.gravity = gravity
        this.orientation = VERTICAL

        // If the bar appears with the bottom, then the shadow needs to be added to the top of it,
        // thus, before the inflation of the bar
        if (castShadow && gravity == BOTTOM) {
            castShadow(ShadowView.ShadowType.TOP, shadowStrength)
        }

        // The layout is already inflated via binding, so no need to call inflate again

        // If the bar appears with the top, then the shadow needs to be added to the bottom of it,
        // thus, after the inflation of the bar
        if (castShadow && gravity == TOP) {
            castShadow(ShadowView.ShadowType.BOTTOM, shadowStrength)
        }
    }

    internal fun adjustWithPositionAndOrientation(activity: Activity, gravity: Gravity) {
        val flashbarViewLp = RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        val statusBarHeight = activity.getStatusBarHeightInPx()

        val flashbarViewContentLp = binding.fbContent.layoutParams as LinearLayout.LayoutParams

        when (gravity) {
            TOP -> {
                flashbarViewContentLp.topMargin = statusBarHeight + (TOP_COMPENSATION_MARGIN / 2)
                flashbarViewLp.addRule(RelativeLayout.ALIGN_PARENT_TOP)
            }
            BOTTOM -> {
                flashbarViewContentLp.bottomMargin = BOTTOM_COMPENSATION_MARGIN
                flashbarViewLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            }
        }
        binding.fbContent.layoutParams = flashbarViewContentLp
        layoutParams = flashbarViewLp
    }

    internal fun addParent(flashbarContainerView: FlashbarContainerView) {
        this.parentFlashbarContainer = flashbarContainerView
    }

    internal fun setBarBackgroundDrawable(drawable: Drawable?) {
        if (drawable == null) return

        if (SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            binding.fbRoot.background = drawable
        } else {
            binding.fbRoot.setBackgroundDrawable(drawable)
        }
    }

    internal fun setBarBackgroundColor(@androidx.annotation.ColorInt color: Int?) {
        if (color == null) return
        binding.fbRoot.setBackgroundColor(color)
    }

    internal fun setBarTapListener(listener: Flashbar.OnTapListener?) {
        if (listener == null) return

        binding.fbRoot.setOnClickListener {
            listener.onTap(parentFlashbarContainer.parentFlashbar)
        }
    }

    internal fun setTitle(title: String?) {
        if (TextUtils.isEmpty(title)) return

        binding.fbTitle.text = title
        binding.fbTitle.visibility = VISIBLE
    }

    internal fun setTitleSpanned(title: Spanned?) {
        if (title == null) return

        binding.fbTitle.text = title
        binding.fbTitle.visibility = VISIBLE
    }

    internal fun setTitleTypeface(typeface: Typeface?) {
        if (typeface == null) return
        binding.fbTitle.typeface = typeface
    }

    internal fun setTitleSizeInPx(size: Float?) {
        if (size == null) return
        binding.fbTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
    }

    internal fun setTitleSizeInSp(size: Float?) {
        if (size == null) return
        binding.fbTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
    }

    internal fun setTitleColor(color: Int?) {
        if (color == null) return
        binding.fbTitle.setTextColor(color)
    }

    internal fun setTitleAppearance(titleAppearance: Int?) {
        if (titleAppearance == null) return

        if (SDK_INT >= M) {
            binding.fbTitle.setTextAppearance(titleAppearance)
        } else {
            binding.fbTitle.setTextAppearance(context, titleAppearance)
        }
    }

    internal fun setMessage(message: String?) {
        if (TextUtils.isEmpty(message)) return

        binding.fbMessage.text = message
        binding.fbMessage.visibility = VISIBLE
    }

    internal fun setMessageSpanned(message: Spanned?) {
        if (message == null) return

        binding.fbMessage.text = message
        binding.fbMessage.visibility = VISIBLE
    }

    internal fun setMessageTypeface(typeface: Typeface?) {
        if (typeface == null) return
        binding.fbMessage.typeface = typeface
    }

    internal fun setMessageSizeInPx(size: Float?) {
        if (size == null) return
        binding.fbMessage.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
    }

    internal fun setMessageSizeInSp(size: Float?) {
        if (size == null) return
        binding.fbMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
    }

    internal fun setMessageColor(color: Int?) {
        if (color == null) return
        binding.fbMessage.setTextColor(color)
    }

    internal fun setMessageAppearance(messageAppearance: Int?) {
        if (messageAppearance == null) return

        if (SDK_INT >= M) {
            binding.fbMessage.setTextAppearance(messageAppearance)
        } else {
            binding.fbMessage.setTextAppearance(context, messageAppearance)
        }
    }

    internal fun setPrimaryActionText(text: String?) {
        if (TextUtils.isEmpty(text)) return

        binding.fbPrimaryAction.text = text
        binding.fbPrimaryAction.visibility = VISIBLE
    }

    internal fun setPrimaryActionTextSpanned(text: Spanned?) {
        if (text == null) return

        binding.fbPrimaryAction.text = text
        binding.fbPrimaryAction.visibility = VISIBLE
    }

    internal fun setPrimaryActionTextTypeface(typeface: Typeface?) {
        if (typeface == null) return
        binding.fbPrimaryAction.typeface = typeface
    }

    internal fun setPrimaryActionTextSizeInPx(size: Float?) {
        if (size == null) return
        binding.fbPrimaryAction.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
    }

    internal fun setPrimaryActionTextSizeInSp(size: Float?) {
        if (size == null) return
        binding.fbPrimaryAction.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
    }

    internal fun setPrimaryActionTextColor(color: Int?) {
        if (color == null) return
        binding.fbPrimaryAction.setTextColor(color)
    }

    internal fun setPrimaryActionTextAppearance(messageAppearance: Int?) {
        if (messageAppearance == null) return

        if (SDK_INT >= M) {
            binding.fbPrimaryAction.setTextAppearance(messageAppearance)
        } else {
            binding.fbPrimaryAction.setTextAppearance(context, messageAppearance)
        }
    }

    internal fun setPrimaryActionTapListener(listener: Flashbar.OnActionTapListener?) {
        if (listener == null) return

        binding.fbPrimaryAction.setOnClickListener {
            listener.onActionTapped(parentFlashbarContainer.parentFlashbar)
        }
    }

    internal fun setPositiveActionText(text: String?) {
        if (TextUtils.isEmpty(text)) return

        binding.fbSecondaryActionContainer.visibility = VISIBLE
        binding.fbPositiveAction.text = text
        binding.fbPositiveAction.visibility = VISIBLE
    }

    internal fun setPositiveActionTextSpanned(text: Spanned?) {
        if (text == null) return

        binding.fbSecondaryActionContainer.visibility = VISIBLE
        binding.fbPositiveAction.text = text
        binding.fbPositiveAction.visibility = VISIBLE
    }

    internal fun setPositiveActionTextTypeface(typeface: Typeface?) {
        if (typeface == null) return
        binding.fbPositiveAction.typeface = typeface
    }

    internal fun setPositiveActionTextSizeInPx(size: Float?) {
        if (size == null) return
        binding.fbPositiveAction.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
    }

    internal fun setPositiveActionTextSizeInSp(size: Float?) {
        if (size == null) return
        binding.fbPositiveAction.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
    }

    internal fun setPositiveActionTextColor(color: Int?) {
        if (color == null) return
        binding.fbPositiveAction.setTextColor(color)
    }

    internal fun setPositiveActionTextAppearance(messageAppearance: Int?) {
        if (messageAppearance == null) return

        if (SDK_INT >= M) {
            binding.fbPositiveAction.setTextAppearance(messageAppearance)
        } else {
            binding.fbPositiveAction.setTextAppearance(context, messageAppearance)
        }
    }

    internal fun setPositiveActionTapListener(listener: Flashbar.OnActionTapListener?) {
        if (listener == null) return

        binding.fbPositiveAction.setOnClickListener {
            listener.onActionTapped(parentFlashbarContainer.parentFlashbar)
        }
    }

    internal fun setNegativeActionText(text: String?) {
        if (TextUtils.isEmpty(text)) return

        binding.fbSecondaryActionContainer.visibility = VISIBLE
        binding.fbNegativeAction.text = text
        binding.fbNegativeAction.visibility = VISIBLE
    }

    internal fun setNegativeActionTextSpanned(text: Spanned?) {
        if (text == null) return

        binding.fbSecondaryActionContainer.visibility = VISIBLE
        binding.fbNegativeAction.text = text
        binding.fbNegativeAction.visibility = VISIBLE
    }

    internal fun setNegativeActionTextTypeface(typeface: Typeface?) {
        if (typeface == null) return
        binding.fbNegativeAction.typeface = typeface
    }

    internal fun setNegativeActionTextSizeInPx(size: Float?) {
        if (size == null) return
        binding.fbNegativeAction.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
    }

    internal fun setNegativeActionTextSizeInSp(size: Float?) {
        if (size == null) return
        binding.fbNegativeAction.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
    }

    internal fun setNegativeActionTextColor(color: Int?) {
        if (color == null) return
        binding.fbNegativeAction.setTextColor(color)
    }

    internal fun setNegativeActionTextAppearance(messageAppearance: Int?) {
        if (messageAppearance == null) return

        if (SDK_INT >= M) {
            binding.fbNegativeAction.setTextAppearance(messageAppearance)
        } else {
            binding.fbNegativeAction.setTextAppearance(context, messageAppearance)
        }
    }

    internal fun setNegativeActionTapListener(listener: Flashbar.OnActionTapListener?) {
        if (listener == null) return

        binding.fbNegativeAction.setOnClickListener {
            listener.onActionTapped(parentFlashbarContainer.parentFlashbar)
        }
    }

    internal fun showIcon(showIcon: Boolean) {
        binding.fbIcon.visibility = if (showIcon) VISIBLE else GONE
    }

    internal fun showIconScale(scale: Float, scaleType: ImageView.ScaleType?) {
        binding.fbIcon.scaleX = scale
        binding.fbIcon.scaleY = scale
        binding.fbIcon.scaleType = scaleType
    }

    internal fun setIconDrawable(icon: Drawable?) {
        if (icon == null) return
        binding.fbIcon.setImageDrawable(icon)
    }

    internal fun setIconBitmap(bitmap: Bitmap?) {
        if (bitmap == null) return
        binding.fbIcon.setImageBitmap(bitmap)
    }

    internal fun setIconColorFilter(colorFilter: Int?, filterMode: PorterDuff.Mode?) {
        if (colorFilter == null) return
        if (filterMode == null) {
            binding.fbIcon.setColorFilter(colorFilter)
        } else {
            binding.fbIcon.setColorFilter(colorFilter, filterMode)
        }
    }

    internal fun startIconAnimation(animator: FlashAnimIconBuilder?) {
        animator?.withView(binding.fbIcon)?.build()?.start()
    }

    internal fun stopIconAnimation() {
        binding.fbIcon.clearAnimation()
    }

    internal fun enableSwipeToDismiss(enable: Boolean, callbacks: DismissCallbacks) {
        if (enable) {
            binding.fbRoot.setOnTouchListener(SwipeDismissTouchListener(this, callbacks))
        }
    }

    internal fun setProgressPosition(position: Flashbar.ProgressPosition?) {
        if (position == null) return
        when (position) {
            LEFT -> {
                binding.fbLeftProgress.visibility = VISIBLE
                binding.fbRightProgress.visibility = GONE
            }
            RIGHT -> {
                binding.fbLeftProgress.visibility = GONE
                binding.fbRightProgress.visibility = VISIBLE
            }
        }
    }

    internal fun setProgressTint(progressTint: Int?, position: Flashbar.ProgressPosition?) {
        if (position == null || progressTint == null) return

        val progressBar = when (position) {
            LEFT -> binding.fbLeftProgress
            RIGHT -> binding.fbRightProgress
        }

        progressBar.setBarColor(progressTint)
    }

    private fun castShadow(shadowType: ShadowView.ShadowType, strength: Int) {
        val params = RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, context.convertDpToPx(strength))
        val shadow = ShadowView(context)
        shadow.applyShadow(shadowType)
        addView(shadow, params)
    }
}
