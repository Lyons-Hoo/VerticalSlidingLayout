package com.example.lyons.demo.customerview;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Scroller;

/**
 * Created by Lyons on 2016/10/29.
 */

public class VerticalSlidingLayout extends ViewGroup {

    private final String TAG = "VerticalSlidingLayout";

    /**
     * 滚动器
     */
    private Scroller mScroller;
    /**
     * 速率器（指定时间内运动的像素值）
     */
    private VelocityTracker mTracker;

    /**
     * 记录当前View下标
     */
    private int mCurrentIndex;

    /**
     * View下标改变监听器
     */
    private SlidingViewGroup.OnViewIndexChangeListener mListener;

    public void setOnViewIndexChangeListener(SlidingViewGroup.OnViewIndexChangeListener mListener) {
        this.mListener = mListener;
    }

    public VerticalSlidingLayout(Context context) {
        super(context);
        init(context);
    }

    public VerticalSlidingLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
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
        // 实例化Scroller
        mScroller = new Scroller(context);
    }

    /**
     * 对容器内的子View进行测量，来确定容器的宽高
     *
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 获取容器布局参数
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        /**
         * 为了支持Layout的WRAP_CONTENT模式
         * 如果宽或者高使用了WRAP_CONTENT模式，才去计算容器的宽高，否则使用父容器传入的宽高
         */
        if (layoutParams.width == LayoutParams.WRAP_CONTENT || layoutParams.height == LayoutParams.WRAP_CONTENT) {
            int containerWidth = 0; // 容器的宽度
            int containerHeight = 0; // 容器的高度
            int childCount = getChildCount(); // 获取容器内子View的个数
            for (int i = 0; i < childCount; i++) { // 遍历子View
                View childView = getChildAt(i); // 获取当前子View
                // 测量当前子View
                measureChild(childView, widthMeasureSpec, heightMeasureSpec);
                // 获取子View的Margin信息
                MarginLayoutParams childLayoutParams = (MarginLayoutParams) childView.getLayoutParams();
                // 计算当前子View的实际宽度（包含Margin和Padding信息）
                int childViewWidth = childLayoutParams.leftMargin + childView.getPaddingLeft()
                        + childView.getMeasuredWidth() + childView.getPaddingRight() + childLayoutParams.rightMargin;
                // 计算当前子View的实际高度（同样包含Margin和Padding信息）
                int childViewHeight = childLayoutParams.topMargin + childView.getPaddingTop()
                        + childView.getMeasuredHeight() + childView.getPaddingBottom() + childLayoutParams.bottomMargin;
                // 找出最宽的子View，作为容器的宽度
                containerWidth = childViewWidth > containerWidth ? childViewWidth : containerWidth;
                containerHeight += childViewHeight; // 累加子View的高度，计算出容器的高度
            }
            /**
             * 容器宽高测量完毕，根据布局的宽高模式，设置进去
             */
            setMeasuredDimension(layoutParams.width == LayoutParams.WRAP_CONTENT ? containerWidth : widthMeasureSpec,
                    layoutParams.height == LayoutParams.WRAP_CONTENT ? containerHeight : heightMeasureSpec);
        } else {
            /**
             * Layout中没有WRAP_CONTENT模式，直接设置父容器传入的值即可
             */
            setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
        }

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
        /**
         * 说明：r为测量好的容器宽度，b为测量好的容器高度
         * 将容器内的每个字View填充整个屏幕，这样的话意味
         * 着子View设置的一些属性失效（例如Margin和Padding）
         */
        int endLayoutY = 0; // Y轴布局到哪里
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) { // 遍历子View
            View childView = getChildAt(i);
            int startLayoutY = 0;
            /**
             * 对子View进行垂直布局（Vertical）
             */
            startLayoutY += endLayoutY; // 计算每屏Y轴其实布局坐标点
            endLayoutY = startLayoutY + b; // 计算每屏Y轴结束布局坐标点
            childView.layout(l, startLayoutY, r, endLayoutY); // 对子View进行定位
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (null == mTracker) {
            mTracker = VelocityTracker.obtain(); // 实例化速率器
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE: // 手指移动
                if (null != mTracker) {
                    mTracker.addMovement(event); // 将事件传递给速率器
                    mTracker.computeCurrentVelocity(40); // 设置速率器时间（单位为毫秒）
                }
                Log.d(TAG, "---------VelocityY:--------  " + mTracker.getYVelocity());
                /**
                 * 随着手指移动，并设置了1/2的阻尼
                 */
                scrollBy(0, (int) (-mTracker.getYVelocity() / 2));
                break;
            case MotionEvent.ACTION_UP: // 手指抬起
                /**
                 *  滑动的距离（getScrollY）+ 屏幕的2/3 高度（因为是上下滑动，
                 *  左右的话就是算宽度）/ 屏幕的高度计算滑动的距离是否超过
                 *  屏幕的2/3，超过就滑动到下一屏（viewIndex从0开始）
                 */
                Log.d(TAG, "---------ScrollY:--------  " + getScrollY());
                Log.d(TAG, "---------Height:--------  " + getHeight());
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
                int maxChildIndex = getChildCount() - 1;
                if (viewIndex > maxChildIndex) {
                    viewIndex = maxChildIndex;
                }
                /**
                 *  计算要滑动到的Y轴坐标 = viewIndex * 屏幕的高度 - 滑动的距离
                 *  正数：View向上滑动 负数：View向下滑动
                 */
                int wantScrollY = viewIndex * getHeight() - getScrollY();
//                Log.d(TAG, "---------wantScrollY:------  " + wantScrollY);
                mScroller.startScroll(0, getScrollY(), 0, wantScrollY); // 滑动
                invalidate(); // 重绘UI
                mTracker.recycle(); //　释放速率器
                mTracker = null;
                break;
        }
        return true; // true表示事件会继续往下传递，false则消耗掉事件，不再继续往下传递
    }

    /**
     * 说明：此方法会重复调用invalidate()方法，从而达到滑动效果（可参考Log打印信息）
     * 重写computeScroll()方法，并在其内部完成平滑滚动的逻辑（其实就是滑动，然后重绘）
     */
    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) { // 滑动没有完成
//            Log.d(TAG, "-------ScrollerCurrentY:------  " + mScroller.getCurrY());
            scrollTo(0, mScroller.getCurrY()); // 滑动
            invalidate(); // 重绘
        }
    }

    /**
     * 重写generateLayoutParams()方法，获取View的Margin参数信息
     *
     * @param attrs
     * @return
     */
    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    /**
     * 回调接口，当ViewIndex改变时触发
     */
    public interface OnViewIndexChangeListener {
        void viewIndexChanged(int viewIndex);
    }
}
