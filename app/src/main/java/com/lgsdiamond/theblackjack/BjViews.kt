package com.lgsdiamond.theblackjack

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.*
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.TypefaceSpan
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*


class FontUtility {
    companion object {
        val contentFace: Typeface by lazy {
            Typeface.createFromAsset(gMainActivity.assets,
                    gMainActivity.getString(R.string.content_face_font_path))
        }

        val titleFace: Typeface by lazy {
            Typeface.createFromAsset(gMainActivity.assets,
                    gMainActivity.getString(R.string.title_face_font_path))
        }

        fun customFaceMenu(menu: Menu, menuFace: Typeface) {

            fun applyFontToMenuItem(mi: MenuItem) {
                val mNewTitle = SpannableString(mi.title)
                mNewTitle.setSpan(CustomTypefaceSpan("", menuFace), 0, mNewTitle.length,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                mi.title = mNewTitle
            }

            for (i in 0..(menu.size() - 1)) {
                val menuItem = menu.getItem(i)

                val subMenu = menuItem.subMenu
                if ((subMenu != null) && subMenu.size() > 0) {
                    customFaceMenu(subMenu, menuFace)
                }
                applyFontToMenuItem(menuItem)
            }
        }
    }
}

class CustomTypefaceSpan(family: String, private val newType: Typeface) : TypefaceSpan(family) {

    override fun updateDrawState(ds: TextPaint) {
        applyCustomTypeFace(ds, newType)
    }

    override fun updateMeasureState(paint: TextPaint) {
        applyCustomTypeFace(paint, newType)
    }

    private fun applyCustomTypeFace(paint: Paint, tf: Typeface) {
        val oldStyle: Int
        val old = paint.typeface

        oldStyle = if (old == null) 0 else old.style

        val fake = oldStyle and tf.style.inv()
        if (fake and Typeface.BOLD != 0) {
            paint.isFakeBoldText = true
        }

        if (fake and Typeface.ITALIC != 0) {
            paint.textSkewX = -0.25f
        }

        paint.typeface = tf
    }
}

class BjTextView : AppCompatTextView {

    // constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
        typeface = FontUtility.titleFace
    }
}

class BjEditText : AppCompatEditText {

    // constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
        typeface = FontUtility.contentFace
    }
}

class BjButton : AppCompatButton {

    // constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
        typeface = FontUtility.titleFace
    }

    fun showProposed() {
        setTextColor(Color.BLUE)
        startAnimation(BjAnimUtility.sButtonEmphAnim)
    }

    fun backToNormal() {
        setTextColor(Color.BLACK)
    }

}

class BjImageButton : AppCompatImageButton {

    // constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

}

class BjCardView : AppCompatImageView {

    // constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

}

class BjCheckBox : AppCompatCheckBox {

    // constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
        typeface = FontUtility.titleFace
    }
}

class BjSwitch : Switch {

    // constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
        typeface = FontUtility.titleFace
    }
}

class BjSpinner : AppCompatSpinner {

    // constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
    }
}

class BjArrayAdapter<T>(context: Context, textViewResourceId: Int, data: Array<T>)
    : ArrayAdapter<T>(context, textViewResourceId, data) {


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        if (view is TextView) {
            view.typeface = FontUtility.titleFace
        }
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        if (view is TextView) {
            view.typeface = FontUtility.titleFace
        }
        return view
    }
}

// TableLayout
class BjGameLayout : ConstraintLayout {
    // constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

}

// BoxLayout
class BjBoxLayout : ConstraintLayout {
    // constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

}

// DealerLayout
class BjDealerLayout : ConstraintLayout {
    // constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

}

// CardsLayout
class BjPlayerHandLayout : FrameLayout {
    // constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

}

// CardsLayout
class BjDealerHandLayout : FrameLayout {
    // constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

}

// Chips
class BjChipsLayout : LinearLayout {
    // constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

}

// ActionsLayout
class BjActionsLayout : LinearLayout {
    // constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

}

// Preference RecyclerView
class BjPrefRecyclerView : RecyclerView {
    // constructor
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

}
