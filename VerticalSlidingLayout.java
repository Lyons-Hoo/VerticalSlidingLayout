package com.example.lyons.demo.customerview;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Scroller;

/**
 * 这是一个可滑动的垂直布局
 * Created by Lyons on 2016/10/29.
 */

public class VerticalSlidingLayout extends ViewGroup {

    private static final String TAG = "VerticalSlidingLayout";

    /**
     * 上下文对象
     */
    private Context mContext;

    /**
     * 滚动器
     */
    private Scroller mScroller;
    /**
     * 速率器（指定时间内运动的像素值）
     */
    private VelocityTracker mTracker;

    /**
     * 标识是否可以滑动
     */
    private boolean mCanSliding;

    /**
     * 记录能滑动到的最大的View下标
     */
    private int mMaxSlidingViewIndex;

    /**
     * 记录当前View下标
     */
    private int mCurrentIndex;

    /**
     * View下标改变监听器
     */
    private OnViewIndexChangeListener mListener;

    public void setOnViewIndexChangeListener(OnViewIndexChangeListener mListener) {
        this.mListener = mListener;
    }

   public VerticalSlidingLayout(Context context) {
        this(context, null);
    }

    public VerticalSlidingLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalSlidingLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public VerticalSlidingLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * 初始化方法
     *
     * @param context
     */
    private void init(Context context) {
        if (null == context) {
            this.mContext = getContext();
        } else {
            this.mContext = context;
        }
        // 实例化Scroller
        mScroller = new Scroller(mContext);
        // 默认屏幕不可滑动
        mCanSliding = false;
        // 默认可滑动的View下标为0
        mMaxSlidingViewIndex = 0;
    }

    /**
     * 对容器内的子View进行测量，来确定容器的宽高
     *
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int containerWidth = 0; // 容器的宽度
        int containerHeight = 0; // 容器的高度
        /**
         * MeasureSpec Mode 说明：
         *          MeasureSpec.EXACTLY：指定容器的尺寸为精确的具体值（例如30dp或者MATCH_PARENT模式）
         *          MeasureSpec.AT_MOST：指定容器的尺寸为最大尺寸（通常是指宽高模式设置为WARP_CONTENT）
         */
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec); // 获取容器宽度的模式
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec); // 获取容器高度的模式
        /**
         * 为了支持Layout的WRAP_CONTENT模式
         * 如果宽或者高使用了WRAP_CONTENT模式，才去计算容器的宽高
         */
        if (widthMode == MeasureSpec.AT_MOST || heightMode == MeasureSpec.AT_MOST) {
            // 获取容器布局参数
            final int childCount = getChildCount(); // 获取容器内子View的个数
            for (int i = 0; i < childCount; i++) { // 遍历子View
                final View childView = getChildAt(i); // 获取当前子View
                // 测量当前子View
                measureChild(childView, widthMeasureSpec, heightMeasureSpec);
                // 获取子View的Margin信息
                final ViewGroup.MarginLayoutParams childLayoutParams = (ViewGroup.MarginLayoutParams) childView.getLayoutParams();
                // 计算当前子View的实际宽度（包含Margin和Padding信息）
                int childViewWidth = childLayoutParams.leftMargin + childView.getPaddingLeft()
                        + childView.getMeasuredWidth() + childView.getPaddingRight() + childLayoutParams.rightMargin;
                // 计算当前子View的实际高度（同样包含Margin和Padding信息）
                int childViewHeight = childLayoutParams.topMargin + childView.getPaddingTop()
                        + childView.getMeasuredHeight() + childView.getPaddingBottom() + childLayoutParams.bottomMargin;
                // 找出最宽的子View，作为容器的宽度
                containerWidth = childViewWidth > containerWidth ? childViewWidth : containerWidth;
                // 累加子View的高度，计算出容器的高度
                containerHeight += childViewHeight;
            }
        }
        /**
         * 容器的宽高有使用WRAP_CONTENT模式，就使用测量的值
         * 否则直接使用父容器建议的宽高
         */
        containerWidth = widthMode == MeasureSpec.AT_MOST ? containerWidth : MeasureSpec.getSize(widthMeasureSpec);
        containerHeight = heightMode == MeasureSpec.AT_MOST ? containerHeight : MeasureSpec.getSize(heightMeasureSpec);
        /**
         * 检查容器设置的宽高是否超过了屏幕宽高
         * 超过则设置容器的宽高为屏幕的宽高
         */
        DisplayMetrics displayMetrics = getDisplayMetrics();
        final int fullScreenWidth = displayMetrics.widthPixels;
        final int fullScreenHeight = displayMetrics.heightPixels;
        containerWidth = Math.min(containerWidth, fullScreenWidth);
        containerHeight = Math.min(containerHeight, fullScreenHeight);
        /**
         * 处理完毕，告诉父容器测量结果
         */
        setMeasuredDimension(containerWidth, containerHeight);
    }

    /**
     * 对容器内的子View进行布局（位置定位）
     *
     * @param changed
     * @param l
     * @param t
     * @param r
     * @param b
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        int endLayoutX = 0; // X轴布局到哪里
        int endLayoutY = 0; // Y轴布局到哪里
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) { // 遍历子View
            final View childView = getChildAt(i);
            if (childView.getVisibility() == View.GONE) { // 有子View设置了隐藏
                continue; // 跳过，进入下一子View
            }
            // 测量当前子View（为了获取Padding和Margin信息）
            measureChild(childView, r, b);
            int childMeasureWidth = childView.getMeasuredWidth();
            int childMeasureHeight = childView.getMeasuredHeight();
            // 获取子View的Margin信息
            final ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) childView.getLayoutParams();
            Log.d("aaa", "left:" + childView.getLeft());
            Log.d("aaa", "paddingLeft:" + childView.getPaddingLeft());
            Log.d("aaa", "marginLeft" + layoutParams.leftMargin);
            /**
             * 如果子View宽高有设置MATCH_PARENT模式，将容器的宽高赋给它
             */
            if (layoutParams.width == ViewGroup.MarginLayoutParams.MATCH_PARENT) {
                childMeasureWidth = r;
            }
            if (layoutParams.height == ViewGroup.MarginLayoutParams.MATCH_PARENT) {
                childMeasureHeight = b;
            }

            // 计算子ViewX轴从哪里开始布局
            int startLayoutX = endLayoutX + layoutParams.leftMargin + childView.getPaddingLeft();
            // 计算子ViewY轴从哪里开始布局
            int startLayoutY = endLayoutY + layoutParams.topMargin + childView.getPaddingTop();
            // 计算子ViewX轴布局到哪里
            endLayoutX = startLayoutX + childMeasureWidth + layoutParams.rightMargin + childView.getPaddingRight();
            // 计算子ViewY轴布局到哪里
            endLayoutY = startLayoutY + childMeasureHeight + layoutParams.bottomMargin + childView.getPaddingBottom();
            // 换行
            startLayoutY += layoutParams.topMargin + childView.getPaddingTop();
            startLayoutX = layoutParams.leftMargin + childView.getPaddingLeft();
            /**
             * 子View的位置都确定了，可以进行布局了
             */
            childView.layout(startLayoutX, startLayoutY, endLayoutX, endLayoutY);
        }
        /**
         * 如果子View总高度超过了容器高度，则分屏布局。
         * 根据最终Y轴坐标点，计算可以滑动的最大View下标
         */
        if (endLayoutY > b) {
            // 下标是从0开始
            mMaxSlidingViewIndex = endLayoutY % b == 0 ? endLayoutY / b - 1 : endLayoutY / b;
            Log.d(TAG, "--------------MaxCanSlidingViewIndex：--------------" + mMaxSlidingViewIndex);
            mCanSliding = true;
        }
    }

    /**
     * 事件拦截
     *
     * @param ev
     * @return
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Log.d(TAG, "---------进入拦截器--------");
        return false; // 默认不拦截，先让子View处理，子View不处理的话再由容器处理
    }

    /**
     * 处理分发下来的事件
     *
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (null == mTracker) {
            mTracker = VelocityTracker.obtain(); // 实例化速率器
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE: // 手指移动
                if (!mCanSliding) { // 是否能够滑动
                    break;
                }
                if (null != mTracker) {
                    mTracker.addMovement(event); // 将事件传递给速率器
                    mTracker.computeCurrentVelocity(40); // 设置速率器时间（单位为毫秒）
                }
                Log.d(TAG, "---------VelocityY:--------  " + mTracker.getYVelocity());
                /**
                 * 随着手指移动，并设置了1/3的阻尼
                 */
                scrollBy(0, (int) (-mTracker.getYVelocity() / 3));
                break;
            case MotionEvent.ACTION_UP: // 手指抬起
                if (!mCanSliding) {
                    break;
                }
                /**
                 *  滑动的距离（getScrollY）+ 屏幕的2/3 高度（因为是上下滑动，
                 *  左右的话就是算宽度）/ 屏幕的高度计算滑动的距离是否超过
                 *  屏幕的1/3，超过就滑动到下一屏（viewIndex从0开始）
                 */
                Log.d(TAG, "---------ScrollY:--------  " + getScrollY());
                int viewIndex = 0;
                if (mTracker.getYVelocity() > 0) { // 向下滑动
                    /**
                     * 取屏幕的1/3加上滑动的Y轴/1屏的高度
                     */
                    viewIndex = (getScrollY() + getHeight() / 3) / getHeight();
                } else { // 向上滑动
                    /**
                     * 取屏幕的2/3加上滑动的Y轴/1屏的高度
                     */
                    viewIndex = (getScrollY() + getHeight() / 3 * 2) / getHeight();
                }
                Log.d(TAG, "---------viewIndex:--------  " + viewIndex);
                /**
                 * 回调View下标改变监听
                 */
                if (null != mListener) {
                    if (viewIndex != mCurrentIndex) {
                        mCurrentIndex = viewIndex;
                        mListener.viewIndexChanged(mCurrentIndex);
                    }
                }
                /**
                 * 计算容器最后一个View的下标，防止滑动超出最后一个View
                 */
                if (viewIndex > mMaxSlidingViewIndex) {
                    viewIndex = mMaxSlidingViewIndex;
                }
                /**
                 *  计算要滑动到的Y轴坐标 = viewIndex * 屏幕的高度 - 滑动的距离
                 *  正数：View向上滑动 负数：View向下滑动
                 */
                final int wantScrollY = viewIndex * getHeight() - getScrollY();
                Log.d(TAG, "---------wantScrollY:------  " + wantScrollY);
                mScroller.startScroll(0, getScrollY(), 0, wantScrollY); // 滑动
                invalidate(); // 重绘UI
                mTracker.recycle(); //　释放速率器
                mTracker = null;
                break;
        }
        return true; //
    }

    /**
     * 说明：此方法会重复调用invalidate()方法，从而达到滑动效果（可参考Log打印信息）
     * 重写computeScroll()方法，并在其内部完成平滑滚动的逻辑（其实就是滑动，然后重绘）
     */
    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) { // 滑动没有完成
            Log.d(TAG, "-------ScrollerCurrentY:------  " + mScroller.getCurrY());
            scrollTo(0, mScroller.getCurrY()); // 滑动
            invalidate(); // 重绘
        }
    }

    /**
     * 获取屏幕信息
     *
     * @return
     */
    private DisplayMetrics getDisplayMetrics() {
        return mContext.getResources().getDisplayMetrics();
    }

    /**
     * 重写generateDefaultLayoutParams()方法，设置默认的布局参数
     *
     * @return
     */
    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new ViewGroup.MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    /**
     * 重写generateLayoutParams()方法，获取View的Margin参数信息
     *
     * @param attrs
     * @return
     */
    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new ViewGroup.MarginLayoutParams(mContext, attrs);
    }

    /**
     * 回调接口，当ViewIndex改变时触发
     */
    public interface OnViewIndexChangeListener {
        void viewIndexChanged(int viewIndex);
    }
}
