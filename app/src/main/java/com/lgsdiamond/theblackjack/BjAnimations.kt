package com.lgsdiamond.theblackjack

import android.view.animation.*

//=== Animations ====
const val CARD_ANIMATION_DURATION = 300L

enum class CardAnimation { NONE, DEALING, PEEKING, OPENING, DISCARDING }

class BjAnimUtility {
    companion object {
        val sCardDealAnim: AnimationSet by lazy { newCardDealAnimation() }
        val sCardPeekAnim: AnimationSet by lazy { newCardPeekAnimation() }
        val sCardDiscardAnim: AnimationSet by lazy { newCardDiscardAnimation() }
        val sScoreTextAnim: AnimationSet by lazy { newScoreTextAnimation() }
        val sButtonEmphAnim: AnimationSet by lazy { newButtonEmphAnimation() }

        fun newCardDealAnimation(): AnimationSet {
            val anim = AnimationSet(true)
            val trans = TranslateAnimation(200.0f, 0.0f, 0.0f, 0.0f)
            val alpha = AlphaAnimation(0.0f, 1.0f)
            val scale = ScaleAnimation(0.7f, 1.0f, 0.7f, 1.0f)

            anim.addAnimation(trans)
            anim.addAnimation(alpha)
            anim.addAnimation(scale)

            anim.duration = CARD_ANIMATION_DURATION
            anim.interpolator = DecelerateInterpolator()

            return anim
        }

        fun newCardPeekAnimation(): AnimationSet {
            val anim = AnimationSet(true)
            val transA = TranslateAnimation(0.0f, 30.0f, 0.0f, 0.0f)
            val transB = TranslateAnimation(30.0f, 0.0f, 0.0f, 0.0f)

            transA.interpolator = DecelerateInterpolator()
            transB.interpolator = AccelerateInterpolator()
            transB.startOffset = (CARD_ANIMATION_DURATION * 0.5).toLong()

            anim.addAnimation(transA)
            anim.addAnimation(transB)

            anim.duration = (CARD_ANIMATION_DURATION * 0.5).toLong()

            return anim
        }

        fun newCardDiscardAnimation(): AnimationSet {
            val anim = AnimationSet(true)
            val alpha = AlphaAnimation(1.0f, 0.0f)
            val scale = ScaleAnimation(1.0f, 0.7f, 1.0f, 0.7f)

            anim.addAnimation(alpha)
            anim.addAnimation(scale)

            anim.duration = CARD_ANIMATION_DURATION
            anim.interpolator = DecelerateInterpolator()

            return anim
        }

        fun newScoreTextAnimation(): AnimationSet {
            val anim = AnimationSet(true)
            val alpha = AlphaAnimation(0.0f, 1.0f)
            val scale = ScaleAnimation(3.0f, 1.0f, 3.0f, 1.0f)

            anim.addAnimation(alpha)
            anim.addAnimation(scale)

            anim.duration = (CARD_ANIMATION_DURATION * 1.2).toLong()
            anim.interpolator = DecelerateInterpolator()

            return anim
        }

        fun newButtonEmphAnimation(): AnimationSet {
            val anim = AnimationSet(true)
            val scale = ScaleAnimation(1.3f, 1.0f, 1.3f, 1.0f,
                    Animation.RELATIVE_TO_PARENT, 0.5f, Animation.RELATIVE_TO_PARENT, 0.5f)
            val alpha = AlphaAnimation(0.8f, 1.0f)
            anim.addAnimation(scale)
            anim.addAnimation(alpha)
            anim.duration = 500L
            anim.interpolator = DecelerateInterpolator()

            return anim
        }
    }
}
