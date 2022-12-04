package com.vdaika.apputils.rating

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.Window
import android.view.animation.AnimationUtils
import android.widget.*
import android.widget.RatingBar.OnRatingBarChangeListener
import androidx.appcompat.app.AppCompatDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.vdaika.apputils.R
import com.vdaika.apputils.rating.RatingDialog.Builder.RatingThresholdClearedListener
import com.vdaika.apputils.rating.RatingDialog.Builder.RatingThresholdFailedListener

class RatingDialog(context: Context, private val builder: Builder) : AppCompatDialog(context), OnRatingBarChangeListener, View.OnClickListener {
    var titleTextView: TextView? = null
    var negativeButtonTextView: TextView? = null
    var positiveButtonTextView: TextView? = null
    var formTitleTextView: TextView? = null
    var formSubmitTextView: TextView? = null
    var formCancelTextView: TextView? = null
    var ratingBarView: RatingBar? = null
    var iconImageView: ImageView? = null
    private var etFeedback: EditText? = null
    private var ratingButtons: LinearLayout? = null
    private var feedbackButtons: LinearLayout? = null
    private val threshold: Float
    private var thresholdPassed = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setContentView(R.layout.dialog_rating)
        titleTextView = findViewById(R.id.dialog_rating_title)
        negativeButtonTextView = findViewById(R.id.dialog_rating_button_negative)
        positiveButtonTextView = findViewById(R.id.dialog_rating_button_positive)
        formTitleTextView = findViewById(R.id.dialog_rating_feedback_title)
        formSubmitTextView = findViewById(R.id.dialog_rating_button_feedback_submit)
        formCancelTextView = findViewById(R.id.dialog_rating_button_feedback_cancel)
        ratingBarView = findViewById(R.id.dialog_rating_rating_bar)
        iconImageView = findViewById(R.id.dialog_rating_icon)
        etFeedback = findViewById(R.id.dialog_rating_feedback)
        ratingButtons = findViewById(R.id.dialog_rating_buttons)
        feedbackButtons = findViewById(R.id.dialog_rating_feedback_buttons)
        init()
    }

    private fun init() {
        titleTextView!!.text = builder.title
        positiveButtonTextView!!.text = builder.positiveText
        negativeButtonTextView!!.text = builder.negativeText
        formTitleTextView!!.text = builder.formTitle
        formSubmitTextView!!.text = builder.submitText
        formCancelTextView!!.text = builder.cancelText
        etFeedback!!.hint = builder.feedbackFormHint
        val d = context.packageManager.getApplicationIcon(context.applicationInfo)
        iconImageView!!.setImageDrawable(if (builder.drawable != null) builder.drawable else d)
        ratingBarView!!.onRatingBarChangeListener = this
        positiveButtonTextView!!.setOnClickListener(this)
        negativeButtonTextView!!.setOnClickListener(this)
        formSubmitTextView!!.setOnClickListener(this)
        formCancelTextView!!.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        if (view.id == R.id.dialog_rating_button_negative) {
            dismiss()
        } else if (view.id == R.id.dialog_rating_button_positive) {
            if (ratingBarView!!.rating >= threshold) {
                thresholdPassed = true
                if (builder.ratingThresholdClearedListener == null) {
                    setRatingThresholdClearedListener()
                }
                builder.ratingThresholdClearedListener!!.onThresholdCleared(this, ratingBarView!!.rating, thresholdPassed)
                dismiss()
            } else {
                thresholdPassed = false
                if (builder.ratingThresholdFailedListener == null) {
                    setRatingThresholdFailedListener()
                }
                builder.ratingThresholdFailedListener!!.onThresholdFailed(this, ratingBarView!!.rating, thresholdPassed)
            }
            if (builder.ratingDialogListener != null) {
                builder.ratingDialogListener!!.onRatingSelected(ratingBarView!!.rating, thresholdPassed)
            }
        } else if (view.id == R.id.dialog_rating_button_feedback_submit) {
            val feedback = etFeedback!!.text.toString().trim { it <= ' ' }
            if (TextUtils.isEmpty(feedback)) {
                val shake = AnimationUtils.loadAnimation(context, R.anim.shake)
                etFeedback!!.startAnimation(shake)
                return
            }
            if (builder.ratingDialogFormListener != null) {
                builder.ratingDialogFormListener!!.onFormSubmitted(feedback)
            }
            dismiss()
        } else if (view.id == R.id.dialog_rating_button_feedback_cancel) {
            dismiss()
        }
    }

    override fun onRatingChanged(ratingBar: RatingBar, v: Float, b: Boolean) {}
    private fun setRatingThresholdClearedListener() {
        builder.ratingThresholdClearedListener = object : RatingThresholdClearedListener {
            override fun onThresholdCleared(ratingDialog: RatingDialog?, rating: Float, thresholdCleared: Boolean) {
                openPlayStore(context)
                dismiss()
            }
        }
    }

    private fun setRatingThresholdFailedListener() {
        builder.ratingThresholdFailedListener = object : RatingThresholdFailedListener {
            override fun onThresholdFailed(ratingDialog: RatingDialog?, rating: Float, thresholdCleared: Boolean) {
                openForm()
            }
        }
    }

    private fun openForm() {
        formTitleTextView!!.visibility = View.VISIBLE
        etFeedback!!.visibility = View.VISIBLE
        feedbackButtons!!.visibility = View.VISIBLE
        ratingButtons!!.visibility = View.GONE
        iconImageView!!.visibility = View.GONE
        titleTextView!!.visibility = View.GONE
        ratingBarView!!.visibility = View.GONE
    }

    private fun openPlayStore(context: Context) {
        val marketUri = Uri.parse(builder.playStoreUrl)
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, marketUri))
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(context, "Couldn't find PlayStore on this device", Toast.LENGTH_SHORT).show()
        }
    }

    class Builder(private val context: Context) {
        var title: String? = null
        var positiveText: String? = null
        var negativeText: String? = null
        var playStoreUrl: String = "market://details?id=" + context.packageName
        var formTitle: String? = null
        var submitText: String? = null
        var cancelText: String? = null
        var feedbackFormHint: String? = null
        var ratingThresholdClearedListener: RatingThresholdClearedListener? = null
        var ratingThresholdFailedListener: RatingThresholdFailedListener? = null
        var ratingDialogFormListener: RatingDialogFormListener? = null
        var ratingDialogListener: RatingDialogListener? = null
        var drawable: Drawable? = null
        var threshold = 1f

        interface RatingThresholdClearedListener {
            fun onThresholdCleared(ratingDialog: RatingDialog?, rating: Float, thresholdCleared: Boolean)
        }

        interface RatingThresholdFailedListener {
            fun onThresholdFailed(ratingDialog: RatingDialog?, rating: Float, thresholdCleared: Boolean)
        }

        interface RatingDialogFormListener {
            fun onFormSubmitted(feedback: String?)
        }

        interface RatingDialogListener {
            fun onRatingSelected(rating: Float, thresholdCleared: Boolean)
        }

        private fun initText() {
            title = context.getString(R.string.string_common_rating_dialog_experience)
            negativeText = context.getString(R.string.string_common_rating_dialog_maybe_later)
            positiveText = context.getString(R.string.string_common_rating_dialog_submit)
            formTitle = context.getString(R.string.string_common_rating_dialog_feedback_title)
            submitText = context.getString(R.string.string_common_rating_dialog_submit)
            cancelText = context.getString(R.string.string_common_rating_dialog_cancel)
            feedbackFormHint = context.getString(R.string.string_common_rating_dialog_suggestions)
        }

        fun threshold(threshold: Float): Builder {
            this.threshold = threshold
            return this
        }

        fun onRatingBarFormSumbit(ratingDialogFormListener: RatingDialogFormListener?): Builder {
            this.ratingDialogFormListener = ratingDialogFormListener
            return this
        }

        fun build(): RatingDialog {
            return RatingDialog(context, this)
        }

        init {
            // Set default PlayStore URL
            initText()
        }
    }

    init {
        threshold = builder.threshold
    }
}