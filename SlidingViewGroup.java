package com.example.lyons.demo.customerview;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

//import android.support.v4.view.ViewConfigurationCompat;

/**
 * Created by Lyons on 2016/10/27.
 */

public class SlidingViewGroup extends ViewGroup {

    private final static String TAG = "SlidingViewGroup";

    /**
     * 滚动器
     */
    private Scroller mScroller;
    /**
     * 速率器（指定时间内运动的像素值）
     */
    private VelocityTracker mTracker;

    /**
     * 手指按下时屏幕Y轴坐标
     */
    private float mOnFingerDownY;

    /**
     * 手指移动时屏幕Y轴坐标
     */
    private float mOnFingerMoveY;

    /**
     * 手指抬起时屏幕Y轴坐标
     */
    private float mOnFingerUpY;

    /**
     * 上次触发ACTION_MOVE事件时屏幕Y轴坐标
     */
    private float mLastMoveY;

    /**
     * 手指拖动的最小像素值
     */
    private int mTouchSlop;

    /**
     * 上边界检查
     */
    private int mTopBorder;

    /**
     * 下边界检查
     */
    private int mBottomBorder;

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

    public SlidingViewGroup(Context context) {
        super(context);
        init(context);
    }

    public SlidingViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SlidingViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public SlidingViewGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        // 实例化Scroller
        mScroller = new Scroller(context);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        // 获取TouchSlop的值
//        mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
    }

    /**
     * 对容器内的子View进行测量，来确定容器的宽高
     *
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // 获取容器布局参数
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        // 初始化边界检查值
        mTopBorder = getChildAt(0).getTop(); // 上边界
        mBottomBorder = getChildAt(getChildCount() - 1).getBottom(); // 下边界

        /**
         * 为了支持Layout的WRAP_CONTENT模式
         * 如果宽或者高使用了WRAP_CONTENT模式，才去计算容器的宽高，否则使用父容器传入的宽高
         */
        if (layoutParams.width == LayoutParams.WRAP_CONTENT || layoutParams.height == LayoutParams.WRAP_CONTENT) {
            int containerWidth = 0; // 容器的宽度
            int containerHeight = 0; // 容器的高度
            // 屏幕的宽度
//            int screentWidth = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth();
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
            // Layout中没有WRAP_CONTENT模式，直接设置父容器传入的值即可
            setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
        }
    }

    /**
     * 对容器内的子View进行布局
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
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            int startLayoutY = 0;
            /**
             * 对子View进行垂直布局（Vertical）
             */
            startLayoutY += endLayoutY; // 计算每屏Y轴其实布局坐标点
            endLayoutY = startLayoutY + b; // 计算每屏Y轴结束布局坐标点
            childView.layout(l, startLayoutY, r, endLayoutY); // 对子View进行定位
        }
        /**
         * 以下注释部分实现了简单的LinearLayout的Vertical效果
         */
//        int endLayoutX = 0; // X轴布局到哪里
//        int endLayoutY = 0; // Y轴布局到哪里
//        int childCount = getChildCount();
//        for (int i = 0; i < childCount; i++) { // 遍历子View
//            View childView = getChildAt(i);
//            if (childView.getVisibility() == View.GONE) { // 有子View设置了隐藏
//                continue; // 跳过，进入下一子View
//            }
//            // 测量当前子View
//            measureChild(childView, r, b);
//            // 获取子View的Margin信息
//            MarginLayoutParams layoutParams = (MarginLayoutParams) childView.getLayoutParams();
//            // 计算子ViewX轴从哪里开始布局
//            int startLayoutX = endLayoutX + layoutParams.leftMargin + childView.getPaddingLeft();
//            // 计算子ViewY轴从哪里开始布局
//            int startLayoutY = endLayoutY + layoutParams.topMargin + childView.getPaddingTop();
//            // 计算子ViewX轴布局到哪里
//            endLayoutX = startLayoutX + childView.getMeasuredWidth() + layoutParams.rightMargin + childView.getPaddingRight();
//            // 计算子ViewY轴布局到哪里
//            endLayoutY = startLayoutY + childView.getMeasuredHeight() + layoutParams.bottomMargin + childView.getPaddingBottom();
//            /**
//             * 如果绘制的X轴坐标超出容器，则换行布局
//             */
////            if (endLayoutX > r) {
//            startLayoutY += layoutParams.topMargin + childView.getPaddingTop(); // 换行
//            startLayoutX = layoutParams.leftMargin + childView.getPaddingLeft(); //
////            }
//            /**
//             * 子View的位置都确定了，可以进行布局了
//             */
//            childView.layout(startLayoutX, startLayoutY, endLayoutX, endLayoutY);
//        }
    }

//    @Override
//    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        switch (ev.getAction()) {
//            case MotionEvent.ACTION_DOWN:
//                mOnFingerDownY = ev.getRawY();
//                mLastMoveY = mOnFingerDownY;
//                break;
//            case MotionEvent.ACTION_MOVE:
//                mOnFingerMoveY = ev.getRawY();
//                Log.d(TAG, "拖动时Y轴坐标：" + mOnFingerMoveY);
//                float moveDistance = Math.abs(mOnFingerMoveY - mOnFingerDownY);
//                mLastMoveY = mOnFingerMoveY;
//                /**
//                 * 当手指拖动的距离大于TouchSlop时，认为应该滚动，拦截子View的事件
//                 */
//                if (moveDistance > mTouchSlop) {
//                    return true;
//                }
//                break;
//        }
//        return super.onInterceptTouchEvent(ev);
//    }

    /**
     * 参考郭霖代码，未实现想要的效果（没有什么用）
     *
     * @param event
     * @return
     */
//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_MOVE:
//                mOnFingerMoveY = event.getRawY();
//                int scrolledY = (int) (mLastMoveY - mOnFingerMoveY);
//                if (getScrollY() + scrolledY < mTopBorder) {
//                    scrollTo(0, mTopBorder);
//                    return true;
//                } else if (getScrollY() + getHeight() + scrolledY > mBottomBorder) {
//                    scrollTo(0, mBottomBorder - getHeight());
//                    return true;
//                }
//                scrollBy(0, scrolledY);
//                mLastMoveY = mOnFingerMoveY;
//                break;
//            case MotionEvent.ACTION_UP:
//                // 当手指抬起时，根据当前的滚动值来判定应该滚动到哪个子控件的界面
//                int targetIndex = (getScrollY() + getHeight() / 2) / getHeight();
//                int dy = targetIndex * getHeight() - getScrollY();
//                // 第二步，调用startScroll()方法来初始化滚动数据并刷新界面
//                mScroller.startScroll(0, getScrollY(), 0, dy);
//                invalidate();
//                break;
//        }
//        return super.onTouchEvent(event);
//    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (null == mTracker) {
            mTracker = VelocityTracker.obtain();
        }
        switch (event.getAction()) {
//            case MotionEvent.ACTION_DOWN:
            /** 说一下：
             *  getRawY 是获取手指按下时相对于屏幕顶点坐标系的Y轴坐标点
             *  getY是获取相对于View的的Y轴坐标点
             */
//                mOnFingerDownY = event.getY(); // 记录手指按下时Y轴坐标
//                Log.d(TAG, "手指按下时Y轴坐标：" + mOnFingerDownY);
//                if (null != mTracker) {
//                    mTracker.addMovement(event);
//                }
//                if (!mScroller.isFinished()) {
//                    mScroller.abortAnimation();
//                }
//                break;
            case MotionEvent.ACTION_MOVE: // 手指移动
                if (null != mTracker) {
                    mTracker.addMovement(event);
                    mTracker.computeCurrentVelocity(30);
                }
//                mOnFingerMoveY = event.getY(); // 记录手指移动时Y轴坐标
//                Log.d(TAG, "-----手指滑动时Y轴坐标：----" + mOnFingerMoveY);
//                float moveDistance = Math.abs(mOnFingerMoveY - mOnFingerDownY); // 计算手指移动的距离
//                if (getScrollY() + moveDistance < mTopBorder) {
//                    Log.d(TAG, "ScrollerY:" + getScrollY());
//                    Log.d(TAG, "moveDistance:" + moveDistance);
//                    Log.d(TAG, "--------底部--------");
//                    scrollTo(0, mBottomBorder - getHeight());
//                } else if (getScrollY() + getHeight() + moveDistance > mBottomBorder) {
//                    Log.d(TAG, "--------顶部--------");
//                    scrollTo(0, mTopBorder);
//                }
//                if (moveDistance <= 200) {
                /**
                 * 随着手指移动，并设置了1.8分之1的阻尼
                 */
                scrollBy(0, (int) (-mTracker.getYVelocity() / 1.8));
                /**
                 * 下面是自己去算的一种实现，效果没有借助系统的速率器效果好
                 */
//                scrollBy(0, (int) (-((mOnFingerMoveY - mOnFingerDownY) / 20)));
//                }
                break;
            case MotionEvent.ACTION_UP: // 手指抬起
//                mOnFingerUpY = event.getRawY();
//                Log.d(TAG, "移动了" + (mOnFingerUpY - mOnFingerDownY) + "个像素值");
//                Log.d(TAG, "OnFingerUp_SrollY:" + getScrollY());
//                if (null != mTracker) {
//                    mTracker.addMovement(event); // 将事件源传递给速率器
//                    mTracker.computeCurrentVelocity(1000); //
//                    Log.d(TAG, "1秒内滑动了：" + mTracker.getYVelocity() + "个像素值");
                /**
                 *  滑动的距离（getScrollY）+ 半屏的高度（因为是上下滑动，
                 *  左右的话就是算宽度）/ 屏幕的高度计算滑动的距离是否超过
                 *  屏幕的一半，超过一半就滑动到下一屏（viewIndex从0开始）
                 */
//                int viewIndex = (getScrollY() + getHeight() / 2) / getHeight();
                int viewIndex = (getScrollY() + getHeight() / 4 * 3) / getHeight();
                mCurrentIndex = viewIndex;
                /**
                 * 回调View下标改变监听
                 */
                if (null != mListener) {
                    if (viewIndex != mCurrentIndex) {
                        mListener.viewIndexChanged(mCurrentIndex);
                    }
                }
                Log.d(TAG, "-------viewIndex:" + viewIndex);
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
                Log.d(TAG, "---------wantScrollY:" + wantScrollY);
                mScroller.startScroll(0, getScrollY(), 0, wantScrollY); // 滑动
                invalidate(); // 重绘UI
                mTracker.recycle(); //　释放速率器
                mTracker = null;
//                }
                break;
        }
        return true; // true表示事件会继续往下传递，false则消耗掉事件
    }

    /**
     * 重写computeScroll()方法，并在其内部完成平滑滚动的逻辑（其实就是滑动，然后重绘）
     */
    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            invalidate();
        }
    }

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

    /**
     * 方向枚举
     */
    private enum Direction {

        /**
         * 上方，下方，未知
         */
        DIRECTION_UP, DIRECTION_DOWN, DIRECTION_UNKNOWN;

        /**
         * 根据手指按下和抬起时的坐标计算手势方向
         *
         * @param onDownY
         * @param onUpY
         * @return
         */
        public static Direction getDirection(float onDownY, float onUpY) {
            if ((onUpY - onDownY) > 150) { //
                Log.d(TAG, "---------向下-------");
                return DIRECTION_DOWN;
            } else if ((onDownY - onUpY) > 150) {
                Log.d(TAG, "---------向上-------");
                return DIRECTION_UP;
            }
            return DIRECTION_UNKNOWN;
        }
    }
}
