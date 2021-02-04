package io.ysq.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.*
import kotlin.math.abs

class SwipeMenuLayout @JvmOverloads constructor(context: Context, attrSet: AttributeSet? = null, defStyleAttr: Int = 0) : ViewGroup(context, attrSet, defStyleAttr) {

    private var isSwipeEnable = true              //是否开启侧滑功能
    private var isIos = true                      //iOS、QQ式交互，默认开启
    private var isLeftSwipe = true                //默认左滑，false时右滑

    private var mScaleTouchSlop  = 0    //处理单击事件冲突
    private var mMaxVelocity  = 0       //计算滑动速度用
    private var mPointerId  = 0         //多点触摸只算第一次接触的速度
    private var mHeight = 0                 //自己的高度
    private var mRightMenuWidth = 0         //右侧菜单宽度总和（最大滑动距离）
    private var mLimit = 0                  //滑动判定临界值
    private var mContentView: View? = null  //存储contentView
    private val mLastP = PointF()           //上一次的xy
    private val mFirstP = PointF()          //up-down的坐标, 判断是否是滑动，如果是，则屏蔽一切点击事件
    private var isUnMoved = true            //仿QQ、iOS，滑动时false，单击时true
    private var isUserSwiped = false        //判断是否是滑动，如果是，则屏蔽一切点击事件
    private var isTouching = false          //防止多点触控
    private var mVelocityTracker: VelocityTracker? = null   //滑动速度
    private var iosInterceptFlag = false    //iOS交互下，是否拦截事件

    companion object {
        private var mViewCache: SwipeMenuLayout? = null //存储当前正在滑动的View
    }



    /* 是否开启侧滑功能 */
    fun isSwipeEnable() = isSwipeEnable
    fun setSwipeEnable(swipeEnable: Boolean) = this.apply { isSwipeEnable = swipeEnable }

    /* 是否开启iOS阻塞式交互 */
    fun isIos() = isIos
    fun setIos(ios: Boolean) = this.apply { isIos = ios }

    /* 是否开启左滑 */
    fun isLeftSwipe() = isLeftSwipe
    fun setLeftSwipe(leftSwipe: Boolean) = this.apply { isLeftSwipe = leftSwipe }


    init {
        val vc = ViewConfiguration.get(context)
        mScaleTouchSlop = vc.scaledTouchSlop
        mMaxVelocity = vc.scaledMaximumFlingVelocity
        val ta = context.theme.obtainStyledAttributes(attrSet, R.styleable.SwipeMenuLayout, defStyleAttr, 0)
        val count = ta.indexCount
        for (i in 0 until count) {
            val attr = ta.getIndex(i)
            //如果引用成AndroidLib，资源不是常量，无法使用switch case
            if (attr == R.styleable.SwipeMenuLayout_swipeEnable) {
                isSwipeEnable = ta.getBoolean(i, true)
            } else if (attr == R.styleable.SwipeMenuLayout_ios) {
                isIos = ta.getBoolean(i, true)
            } else if (attr == R.styleable.SwipeMenuLayout_leftSwipe) {
                isLeftSwipe = ta.getBoolean(i, true)
            }
        }
        ta.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        isClickable = true  //令自己可以被点击，从而获得触摸事件

        //由于ViewHolder的复用机制，每次这里要手动恢复初始值
        mRightMenuWidth = 0
        mHeight = 0
        var contentWidth = 0
        val childCount = childCount //适配GridLayoutManager，将以第一个Item（即ContentItem）的宽度为控件宽度
        //为了子View的高，可以MatchParent（参考的FrameLayout和LinearLayout的Horizontal）
        val measureMatchParentChildren = MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY
        var isNeedMeasureChildHeight = false

        for (i in 0 until childCount) {
            val childView = getChildAt(i)
            //令每一个子View可点击，从而获取触摸事件
            childView.isClickable = true
            if (childView.visibility == GONE) {
                continue
            }
            measureChild(childView, widthMeasureSpec, heightMeasureSpec)
            val lp = childView.layoutParams as MarginLayoutParams
            mHeight = mHeight.coerceAtLeast(childView.measuredHeight)
            if (measureMatchParentChildren && lp.height == LayoutParams.MATCH_PARENT) {
                isNeedMeasureChildHeight = true
            }
            if (i == 0) {    //第一个布局是 主要视图
                mContentView = childView
                contentWidth = childView.measuredWidth
            } else {        //从第二个开始是侧滑的菜单
                mRightMenuWidth += childView.measuredWidth
            }
        }

        //宽度取 主要视图 的宽度
        setMeasuredDimension(paddingLeft + paddingRight + contentWidth, mHeight + paddingTop + paddingBottom)
        mLimit = mRightMenuWidth * 4 / 10   //滑动判断的临界值

        if (isNeedMeasureChildHeight) {     //如果子View的height有MatchParent属性，设置子View高度
            forceUniformHeight(childCount, widthMeasureSpec)
        }
    }

    /**
     * 给MatchParent的子View设置高度
     * @see android.widget.LinearLayout 同名方法
     */
    private fun forceUniformHeight(count: Int, widthMeasureSpec: Int) {
        //以父布局高度构建一个Exactly的测量参数
        val uniformMeasureSpec = MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY)
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility == GONE) {
                continue
            }
            val lp = child.layoutParams as MarginLayoutParams
            if (lp.height == LayoutParams.MATCH_PARENT) {
                val oldWidth = lp.width     //measureChildWidthMargins 这个方法会用到宽，所以要保存一下
                lp.width = child.measuredWidth
                measureChildWithMargins(child, widthMeasureSpec, 0, uniformMeasureSpec, 0)
                lp.width = oldWidth
            }
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val childCount = childCount
        var left = paddingLeft
        var right = paddingRight
        for (i in 0 until childCount) {
            val childView = getChildAt(i)
            if (childView.visibility == GONE) {
                continue
            }
            if (isLeftSwipe || i == 0) {   //第一个子View是内容，宽度设置为全屏
                childView.layout(left, paddingTop, left + childView.measuredWidth, paddingTop + childView.measuredHeight)
                left += childView.measuredWidth
            } else {
                childView.layout(right - childView.measuredWidth, paddingTop, right, paddingTop + childView.measuredHeight)
                right -= childView.measuredWidth
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (!isSwipeEnable) {
            return super.dispatchTouchEvent(ev)
        }
        acquireVelocityTracker(ev)
        val vt = mVelocityTracker
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                isUserSwiped = false
                isUnMoved = true    //仿iOS、QQ，侧滑菜单展开时，点击内容区域，关闭侧滑菜单
                iosInterceptFlag = false    //每次DOWN时，默认是不拦截的
                if (isTouching) {
                    return false
                } else {
                    isTouching = true   //在滑了，在滑了/.\
                }
                mLastP.set(ev.rawX, ev.rawY)
                mFirstP.set(ev.rawX, ev.rawY)

                //如果view和viewCache不一样，则立马让它还原，且把它置为null
                if (mViewCache != null) {
                    if (mViewCache != this) {
                        mViewCache?.smoothClose()
                        iosInterceptFlag = isIos    //拦截滑动事件
                    }
                    //只要有一个侧滑菜单处于打开状态，就不给外层布局上下滑动了
                    parent.requestDisallowInterceptTouchEvent(true)
                }
                //求第一个触点的id
                mPointerId = ev.getPointerId(0)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!iosInterceptFlag) {
                    val gap = mLastP.x - ev.rawX
                    //为了在水平滑动中禁止父类ListView等再竖直滑动
                    if (abs(gap) > 10 || abs(scrollX) > 10) {
                        //修改此处使屏蔽父布局滑动更加灵敏
                        parent.requestDisallowInterceptTouchEvent(true)
                    }
                    if (abs(gap) > mScaleTouchSlop) {
                        isUnMoved = false
                    }
                    scrollBy(gap.toInt(), 0)
                    //越界修正
                    if (isLeftSwipe) {//左滑
                        if (scrollX < 0) {
                            scrollTo(0, 0)
                        }
                        //仿iOS，不用拖出两倍宽度
                        if (scrollX > mRightMenuWidth) {
                            scrollTo(mRightMenuWidth, 0)
                        }
                    } else {//右滑
                        if (scrollX < -mRightMenuWidth) {
                            scrollTo(-mRightMenuWidth, 0)
                        }
                        if (scrollX > 0) {
                            scrollTo(0, 0)
                        }
                    }
                    mLastP.set(ev.rawX, ev.rawY)
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (abs(ev.rawX - mFirstP.x) > mScaleTouchSlop) {
                    isUserSwiped = true
                }
                if (!iosInterceptFlag) {
                    vt?.let {
                        it.computeCurrentVelocity(1000, mMaxVelocity.toFloat())
                        val velocityX = it.getXVelocity(mPointerId)
                        if (abs(velocityX) > 1000) {//滑动速度超过阈值
                            if (velocityX < 1000) {
                                if (isLeftSwipe) {  //左滑
                                    //平滑展开Menu
                                    smoothExpand()
                                } else {
                                    //平滑关闭Menu
                                    smoothClose()
                                }
                            } else {
                                if (isLeftSwipe) {  //左滑
                                    //平滑关闭Menu
                                    smoothClose()
                                } else {
                                    //平滑展开Menu
                                    smoothExpand()
                                }
                            }
                        } else {
                            if (abs(scrollX) > mLimit) {//否则就判断滑动距离
                                //平滑展开Menu
                                smoothExpand()
                            } else {
                                //平滑关闭Menu
                                smoothClose()
                            }
                        }
                    }
                }
                //释放
                releaseVelocityTracker()
                isTouching = false
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (!isSwipeEnable) {
            return super.onInterceptTouchEvent(ev)
        }
        when (ev?.action) {
            MotionEvent.ACTION_MOVE -> {
                //屏蔽滑动时的事件
                if (abs(ev.rawX - mFirstP.x) > mScaleTouchSlop) {
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                //为了在侧滑时屏蔽子View的事件
                if (isLeftSwipe && scrollX > mScaleTouchSlop && ev.x < width -scrollX) {
                    //仿iOS、QQ，点击内容区域，关闭侧滑菜单
                    if (isUnMoved) {
                        smoothClose()
                    }
                    return true //拦截
                } else if (scrollX < -mScaleTouchSlop && ev.x > -scrollX) { //点击范围在菜单外，屏蔽
                    if (isUnMoved) {
                        smoothClose()
                    }
                    return true
                }
                //判断触摸操作起始落点，如果距离属于滑动，屏蔽一切点击事件
                if (isUserSwiped) {
                    return true
                }
            }
        }
        //模仿iOS， 点击其他区域，关闭侧滑菜单
        if (iosInterceptFlag) {
            //iOS模式开启，且当前有菜单的View不是自己的，拦截点击事件给子View
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

    /* 平滑展开 */
    private var mExpandAnim: ValueAnimator? = null
    private var mCloseAnim: ValueAnimator? = null
    private var isExpand = false    //当前是否是展开状态

    private fun smoothExpand() {
        //展开就加入ViewCache
        mViewCache = this@SwipeMenuLayout
        //侧滑菜单展开，屏蔽content长按
        mContentView?.let {
            it.isLongClickable = true
        }

        cancelAnim()
        mExpandAnim = ValueAnimator.ofInt(scrollX, if (isLeftSwipe) mRightMenuWidth else -mRightMenuWidth)
        mExpandAnim?.apply {
            addUpdateListener { scrollTo(it.animatedValue as Int, 0) }
            //仿iOS，不用拖出两倍宽度
//            interpolator = OvershootInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isExpand = true
                }
            })
            duration = 300
        }
        mExpandAnim?.start()
    }
    /**
     * 平滑关闭
     */
    private fun smoothClose() {
        mViewCache == null

        //侧滑菜单展开，屏蔽content长按
        mContentView?.let { it.isLongClickable = true }
        cancelAnim()
        mCloseAnim = ValueAnimator.ofInt(scrollX, 0)
        mCloseAnim?.apply {
            addUpdateListener { scrollTo(it.animatedValue as Int, 0) }
            //仿iOS，不用拖出两倍宽度
//            interpolator = OvershootInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isExpand = false
                }
            })
            duration = 300
        }
        mCloseAnim?.start()
    }

    /*
    每次执行动画之前，都应该取消之前的动画
     */
    private fun cancelAnim() {
        mExpandAnim?.let {
            if (it.isRunning) {
                it.cancel()
            }
        }
        mCloseAnim?.let {
            if (it.isRunning) {
                it.cancel()
            }
        }
    }

    /**
     * 向 VelocityTracker 添加MotionEvent
     */
    private fun acquireVelocityTracker(event: MotionEvent) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        }
        mVelocityTracker?.addMovement(event)
    }

    /**
     * 释放 VelocityTracker
     */
    private fun releaseVelocityTracker() {
        mVelocityTracker?.let {
            it.clear()
            it.recycle()
        }
        mVelocityTracker = null
    }

    /**
     * 每次ViewDetach的时候，判断一下ViewCache是不是自己，如果是自己，关闭侧滑菜单，且ViewCache设置为null
     * 理由：
     * 1.防止内存泄漏，
     * 2.侧滑删除自己后，这个View被Recycler回收，复用，下一个进入屏幕的View的状态应该是普通状态，而不是展开状态
     */
    override fun onDetachedFromWindow() {
        if (mViewCache == this) {
            mViewCache?.smoothClose()
            mViewCache == null
        }
        super.onDetachedFromWindow()
    }

    /**
     * 展开时，禁止长按
     */
    override fun performLongClick(): Boolean {
        if (abs(scrollX) > mScaleTouchSlop) {
            return false
        }
        return super.performLongClick()
    }
}