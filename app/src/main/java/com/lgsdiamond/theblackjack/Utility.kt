package com.lgsdiamond.theblackjack

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.widget.Toast
import java.text.DecimalFormat
import kotlin.math.ceil
import kotlin.math.floor


/**
 * Created by LgsDi on 2018-03-10.
 */

fun Float.toDollarString(): String {
    val dFormat = DecimalFormat("####,###,###.00")
    return ("$" + dFormat.format(this))
}

fun Float.toDollarAmountFloor(): Float {
    val intValue = this * 100
    return (intValue / 100.0f)
}

fun Float.toValidBet(): Float {     // $1 unit
    return if (this >= 0.0) ceil(this) else 0.0f
}

fun Float.toAllinBet(): Float {     // $1 unit
    return if (this >= 0.0) floor(this) else 0.0f
}

fun String.toToastShort() {
    Toast.makeText(gContext, this, Toast.LENGTH_SHORT).show()
}

fun CharSequence.spanTitleFace(): CharSequence {
    return spanFace(FontUtility.titleFace)
}

fun CharSequence.spanContentFace(): CharSequence {
    return spanFace(FontUtility.contentFace)
}

fun CharSequence.spanFace(face: Typeface): CharSequence {
    val span = SpannableString(this)
    span.setSpan(CustomTypefaceSpan("", face), 0, this.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

    return span
}