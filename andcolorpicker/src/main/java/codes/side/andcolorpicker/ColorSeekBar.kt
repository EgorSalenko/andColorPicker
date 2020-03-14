package codes.side.andcolorpicker

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.*
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.util.StateSet
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatSeekBar
import codes.side.andcolorpicker.model.Color
import codes.side.andcolorpicker.model.factory.ColorFactory

@Suppress("ConstantConditionIf")
abstract class ColorSeekBar<C : Color<C>> : AppCompatSeekBar,
  SeekBar.OnSeekBarChangeListener {

  companion object {
    private const val TAG = "ColorSeekBar"
    private const val DEBUG = false
  }

  private val _pickedColor: C
  var pickedColor: C
    get() {
      return colorFactory.createColorFrom(_pickedColor)
    }
    set(value) {
      if (DEBUG) {
        Log.d(
          TAG,
          "currentColor set() called on $this with $value"
        )
      }
      if (_pickedColor == value) {
        return
      }
      updateInternalCurrentColorFrom(value)
      refreshProgressFromCurrentColor()
      refreshProgressDrawable()
      refreshThumb()
      notifyListenersOnColorChanged()
    }

  protected val internalPickedColor: C
    get() {
      return _pickedColor
    }

  // Dirty hack to stop onProgressChanged while playing with min/max
  //private var minUpdating = false
  //private var maxUpdating = false

  private val colorFactory: ColorFactory<C>
  private val colorPickListeners = hashSetOf<OnColorPickListener<C>>()
  private lateinit var thumbDrawableDefaultWrapper: LayerDrawable
  private lateinit var thumbDrawablePressed: GradientDrawable

  // TODO: Rename
  protected val coloringDrawables = hashSetOf<Drawable>()

  // TODO: Make use of JvmOverloads
  constructor(
    colorFactory: ColorFactory<C>,
    context: Context
  ) : super(context) {
    this.colorFactory = colorFactory
    this._pickedColor = colorFactory.create()
    init()
  }

  constructor(
    colorFactory: ColorFactory<C>,
    context: Context,
    attrs: AttributeSet?
  ) : super(
    context,
    attrs
  ) {
    this.colorFactory = colorFactory
    this._pickedColor = colorFactory.create()
    init()
  }

  constructor(
    colorFactory: ColorFactory<C>,
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
  ) : super(
    context,
    attrs,
    defStyleAttr
  ) {
    this.colorFactory = colorFactory
    this._pickedColor = colorFactory.create()
    init()
  }

  private fun init() {
    splitTrack = false

    setOnSeekBarChangeListener(this)

    setupBackground()
    setupProgressDrawable()
    setupThumb()
  }

  private fun setupBackground() {
    background = background.mutate()
      .also {
        if (it is RippleDrawable) {
          // TODO: Set ripple size for pre-M too
          // TODO: Make ripple size configurable?
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val rippleSizePx = resources.getDimensionPixelOffset(R.dimen.acp_thumb_ripple_radius)
            it.radius = rippleSizePx
          }
        }
      }
  }

  private fun setupProgressDrawable() {
    if (DEBUG) {
      Log.d(
        TAG,
        "setupProgressDrawable() called on $this"
      )
    }

    val backgroundPaddingPx = resources.getDimensionPixelOffset(R.dimen.acp_seek_background_padding)

    progressDrawable = LayerDrawable(
      arrayOf(
        GradientDrawable().also {
          it.orientation = GradientDrawable.Orientation.LEFT_RIGHT
          it.cornerRadius =
            resources.getDimensionPixelOffset(R.dimen.acp_seek_background_corner_radius)
              .toFloat()
          it.shape = GradientDrawable.RECTANGLE
          // TODO: Make stroke configurable
          //it.setStroke(
          //  4,
          //  Color.rgb(
          //    192,
          //    192,
          //    192
          //  )
          //)
        }
      )
    ).also {
      it.setLayerInset(
        0,
        backgroundPaddingPx,
        backgroundPaddingPx,
        backgroundPaddingPx,
        backgroundPaddingPx
      )
    }
  }

  private fun setupThumb() {
    val backgroundPaddingPx = resources.getDimensionPixelOffset(R.dimen.acp_seek_background_padding)
    val thumbFullSizePx = resources.getDimensionPixelOffset(R.dimen.acp_thumb_size_full)
    val thumbDefaultSizePx = resources.getDimensionPixelOffset(R.dimen.acp_thumb_size_default)

    val sizeD = thumbFullSizePx - thumbDefaultSizePx
    val sizeDHalf = sizeD / 2

    thumbDrawableDefaultWrapper = LayerDrawable(
      arrayOf(
        GradientDrawable().also {
          it.color = ColorStateList.valueOf(android.graphics.Color.WHITE)
          it.shape = GradientDrawable.OVAL
          it.setSize(
            thumbDefaultSizePx,
            thumbDefaultSizePx
          )
        }
      )
    ).also {
      it.setLayerInset(
        0,
        sizeDHalf,
        sizeDHalf,
        sizeDHalf,
        sizeDHalf
      )
    }

    thumbDrawablePressed = GradientDrawable().also {
      it.color = ColorStateList.valueOf(android.graphics.Color.WHITE)
      it.shape = GradientDrawable.OVAL
      it.setSize(
        thumbFullSizePx,
        thumbFullSizePx
      )
    }

    coloringDrawables.add(thumbDrawableDefaultWrapper)
    coloringDrawables.add(thumbDrawablePressed)

    thumb = AnimatedStateListDrawable().also { animatedStateListDrawable ->
      animatedStateListDrawable.addState(
        intArrayOf(android.R.attr.state_pressed),
        thumbDrawablePressed,
        1
      )
      animatedStateListDrawable.addState(
        StateSet.WILD_CARD,
        thumbDrawableDefaultWrapper,
        0
      )
      //animatedStateListDrawable.addTransition(
      //    0,
      //    1,
      //    AnimationDrawable().also {
      //      it.addFrame(
      //          GradientDrawable().also {
      //            it.setSize(
      //                160,
      //                160
      //            )
      //            it.color = ColorStateList.valueOf(Color.BLACK)
      //          },
      //          1500
      //      )
      //      it.addFrame(
      //          GradientDrawable().also {
      //            it.setSize(
      //                160,
      //                160
      //            )
      //            it.color = ColorStateList.valueOf(Color.BLUE)
      //          },
      //          1500
      //      )
      //    },
      //    true
      //)
    }

    thumbOffset -= backgroundPaddingPx / 2
  }

  protected open fun updateInternalCurrentColorFrom(value: C) {
    if (DEBUG) {
      Log.d(
        TAG,
        "updateInternalCurrentColorFrom() called on $this"
      )
    }
  }

  protected open fun refreshProperties() {
    if (DEBUG) {
      Log.d(
        TAG,
        "refreshProperties() called on $this"
      )
    }
  }

  protected open fun refreshProgressFromCurrentColor() {
    if (DEBUG) {
      Log.d(
        TAG,
        "refreshProgressFromCurrentColor() called on $this"
      )
    }
  }

  protected open fun refreshInternalCurrentColorFromProgress() {
    if (DEBUG) {
      Log.d(
        TAG,
        "refreshInternalCurrentColorFromProgress() called on $this"
      )
    }
  }

  protected open fun refreshProgressDrawable() {
    if (DEBUG) {
      Log.d(
        TAG,
        "refreshProgressDrawable() called on $this"
      )
    }
  }

  protected open fun refreshThumb() {
    if (DEBUG) {
      Log.d(
        TAG,
        "refreshThumb() called on $this"
      )
    }
  }

  fun addListener(listener: OnColorPickListener<C>) {
    colorPickListeners.add(listener)
  }

  fun removeListener(listener: OnColorPickListener<C>) {
    colorPickListeners.remove(listener)
  }

  fun clearListeners() {
    colorPickListeners.clear()
  }

  // TODO: Add (mask) delegating OnSeekBarChangeListener
  override fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener?) {
    if (l != this) {
      throw IllegalStateException("Custom OnSeekBarChangeListener not supported yet")
    }
    super.setOnSeekBarChangeListener(l)
  }

  protected fun notifyListenersOnColorChanged() {
    colorPickListeners.forEach {
      it.onColorChanged(
        this,
        pickedColor,
        progress
      )
    }
  }

  private fun notifyListenersOnColorPicking(fromUser: Boolean) {
    colorPickListeners.forEach {
      it.onColorPicking(
        this,
        pickedColor,
        progress,
        fromUser
      )
    }
  }

  private fun notifyListenersOnColorPicked(fromUser: Boolean) {
    colorPickListeners.forEach {
      it.onColorPicked(
        this,
        pickedColor,
        progress,
        fromUser
      )
    }
  }

  // TODO: Revisit
  override fun onProgressChanged(
    seekBar: SeekBar,
    progress: Int,
    fromUser: Boolean
  ) {
    // TODO: Revisit
    //if (minUpdating || maxUpdating) {
    //  return
    //}

    refreshInternalCurrentColorFromProgress()
    refreshProgressDrawable()
    refreshThumb()
    notifyListenersOnColorPicking(fromUser)

    if (!fromUser) {
      notifyListenersOnColorPicked(fromUser)
    }
  }

  override fun onStartTrackingTouch(seekBar: SeekBar) {
  }

  override fun onStopTrackingTouch(seekBar: SeekBar) {
    notifyListenersOnColorPicked(true)
  }

  // TODO: Rename
  interface OnColorPickListener<C : Color<C>> {
    fun onColorPicking(
      picker: ColorSeekBar<C>,
      color: C,
      value: Int,
      fromUser: Boolean
    )

    fun onColorPicked(
      picker: ColorSeekBar<C>,
      color: C,
      value: Int,
      fromUser: Boolean
    )

    fun onColorChanged(
      picker: ColorSeekBar<C>,
      color: C,
      value: Int
    )
  }
}
