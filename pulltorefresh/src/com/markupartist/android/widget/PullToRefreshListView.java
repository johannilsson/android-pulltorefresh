package com.markupartist.android.widget;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

import com.markupartist.android.widget.pulltorefresh.R;

public class PullToRefreshListView extends ListView implements OnScrollListener {

    private static final int PULL_TO_REFRESH = 1;
    private static final int RELEASE_TO_REFRESH = 2;
    private static final int REFRESHING = 4;

    private static final String TAG = "PullToRefreshListView";
    private OnRefreshListener mOnRefreshListener;
    private LayoutInflater mInflater;
    private LinearLayout mRefreshView;
    private int mCurrentScrollState;
    private int mRefreshViewHeight;
    private TextView mRefreshViewText;
    private ImageView mRefreshViewImage;
    private int mRefreshState;

    private RotateAnimation mFlipAnimation;
    private RotateAnimation mReverseFlipAnimation;
    private RotateAnimation mRotateAnimation;
    private int mRefreshOriginalTopPadding;

    public PullToRefreshListView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Load all of the animations we need in code rather than through XML
        mFlipAnimation = new RotateAnimation(0, 180,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mFlipAnimation.setInterpolator(new LinearInterpolator());
        mFlipAnimation.setDuration(250);
        mFlipAnimation.setFillAfter(true);
        mReverseFlipAnimation = new RotateAnimation(180, 360,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mReverseFlipAnimation.setInterpolator(new LinearInterpolator());
        mReverseFlipAnimation.setDuration(250);
        mReverseFlipAnimation.setFillAfter(true);
        mRotateAnimation = new RotateAnimation(0, 360,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mRotateAnimation.setInterpolator(new LinearInterpolator());
        mRotateAnimation.setDuration(1000);
        mRotateAnimation.setRepeatCount(Animation.INFINITE);

        mInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        mRefreshView = (LinearLayout) mInflater.inflate(
                R.layout.pull_to_refresh_header, null);

        mRefreshViewText =
            (TextView) mRefreshView.findViewById(R.id.pull_to_refresh_text);
        mRefreshViewImage =
            (ImageView) mRefreshView.findViewById(R.id.pull_to_refresh_image);

        mRefreshViewImage.setMinimumHeight(50);
        mRefreshView.setOnClickListener(new OnClickRefreshListener());
        mRefreshOriginalTopPadding = mRefreshView.getPaddingTop();

        mRefreshState = PULL_TO_REFRESH;

        addHeaderView(mRefreshView);

        setOnScrollListener(this);

        measureView(mRefreshView);
        mRefreshViewHeight = mRefreshView.getMeasuredHeight();
    }
    
    protected void onAttachedToWindow() {
    	scrollListBy(mRefreshViewHeight, 0);
    	//setSelection(1);
    }

    /**
     * Register a callback to be invoked when this list should be refreshed.
     * 
     * @param onRefreshListener The callback to run.
     */
    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        mOnRefreshListener = onRefreshListener;
    }

    /**
     * Smoothly scroll by distance pixels over duration milliseconds.
     * 
     * <p>Using reflection internally to call smoothScrollBy for API Level 8
     * otherwise scrollBy is called.
     * 
     * @param distance Distance to scroll in pixels.
     * @param duration Duration of the scroll animation in milliseconds.
     */
    private void scrollListBy(int distance, int duration) {
        try {
            Method method = ListView.class.getMethod("smoothScrollBy",
                    Integer.TYPE, Integer.TYPE);
            method.invoke(this, distance + 1, duration);
        } catch (NoSuchMethodException e) {
            // If smoothScrollBy is not available (< 2.2)
        	setSelection(1);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IllegalAccessException e) {
            System.err.println("unexpected " + e);
        } catch (InvocationTargetException e) {
            System.err.println("unexpected " + e);
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                if (getFirstVisiblePosition() == 0 && mRefreshState != REFRESHING) {
                    if (mRefreshView.getBottom() > mRefreshViewHeight
                            || mRefreshView.getTop() >= 0
                            && mRefreshState == RELEASE_TO_REFRESH) {
                        // Initiate the refresh
                        mRefreshState = REFRESHING;
                        prepareForRefresh();
                        onRefresh();
                    } else if (mRefreshView.getBottom() < mRefreshViewHeight) {
                        // Abort refresh and scroll down below the refresh view
                        int scrollBy = mRefreshView.getBottom();
                        if (mRefreshView.getPaddingTop() > mRefreshOriginalTopPadding) {
                            scrollBy = scrollBy - (mRefreshView.getPaddingTop() - mRefreshOriginalTopPadding);
                        }

                        mRefreshView.setPadding(
                                mRefreshView.getPaddingLeft(),
                                mRefreshOriginalTopPadding,
                                mRefreshView.getPaddingRight(),
                                mRefreshView.getPaddingBottom());

                        scrollListBy(scrollBy, 750);
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                applyHeaderPadding(event);
                break;
        }
        return super.onTouchEvent(event);
    }

    private void applyHeaderPadding(MotionEvent ev) {
        final int historySize = ev.getHistorySize();

        // Workaround for getPointerCount() which is unavailable in 1.5
        // (it's always 1 in 1.5)
        int pointerCount = 1;
        try {
            Method method = MotionEvent.class.getMethod("getPointerCount");
            pointerCount = (Integer)method.invoke(ev);
        } catch (NoSuchMethodException e) {
            pointerCount = 1;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IllegalAccessException e) {
            System.err.println("unexpected " + e);
        } catch (InvocationTargetException e) {
            System.err.println("unexpected " + e);
        }

        for (int h = 0; h < historySize; h++) {
            for (int p = 0; p < pointerCount; p++) {
                if (mRefreshState == RELEASE_TO_REFRESH) {
                    int topPadding = 0;
                    try {
                        // For Android > 2.0
                        Method method = MotionEvent.class.getMethod(
                                "getHistoricalY", Integer.TYPE, Integer.TYPE);
                        topPadding = (int) (((Float) method.invoke(ev, p, h) / 2)
                                - mRefreshViewHeight);
                    } catch (NoSuchMethodException e) {
                        // For Android < 2.0
                        topPadding = (int) (ev.getHistoricalY(h) / 2)
                                - mRefreshViewHeight;
                    } catch (IllegalArgumentException e) {
                        throw e;
                    } catch (IllegalAccessException e) {
                        System.err.println("unexpected " + e);
                    } catch (InvocationTargetException e) {
                        System.err.println("unexpected " + e);
                    }
                    mRefreshView.setPadding(
                            mRefreshView.getPaddingLeft(),
                            topPadding,
                            mRefreshView.getPaddingRight(),
                            mRefreshView.getPaddingBottom());
                }
            }
        }
    }

    private void measureView(View child) {
        ViewGroup.LayoutParams p = child.getLayoutParams();
        if (p == null) {
            p = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        int childWidthSpec = ViewGroup.getChildMeasureSpec(0,
                0 + 0, p.width);
        int lpHeight = p.height;
        int childHeightSpec;
        if (lpHeight > 0) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
        } else {
            childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }
        child.measure(childWidthSpec, childHeightSpec);
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
            int visibleItemCount, int totalItemCount) {
        /*
         * When the refresh view is completely visible, change the text to say
         * "Release to refresh..." and flip the arrow drawable.
         */
        if (mCurrentScrollState == SCROLL_STATE_TOUCH_SCROLL
                && mRefreshState != REFRESHING) {
            mRefreshView.setVisibility(View.VISIBLE);

            if (firstVisibleItem == 0) {
                if ((mRefreshView.getBottom() > mRefreshViewHeight
                        || mRefreshView.getTop() >= 0)
                        && mRefreshState != RELEASE_TO_REFRESH) {
                    mRefreshState = RELEASE_TO_REFRESH;
                    mRefreshViewText.setText(R.string.pull_to_refresh_release_label);
                    mRefreshViewImage.clearAnimation();
                    mRefreshViewImage.startAnimation(mFlipAnimation);
                } else if (mRefreshView.getBottom() < mRefreshViewHeight
                        && mRefreshState != PULL_TO_REFRESH) {
                    mRefreshState = PULL_TO_REFRESH;
                    mRefreshViewText.setText(R.string.pull_to_refresh_pull_label);
                    mRefreshViewImage.clearAnimation();
                    mRefreshViewImage.startAnimation(mReverseFlipAnimation);
                }
            } else {
                if (mRefreshState != PULL_TO_REFRESH) {
                    mRefreshState = PULL_TO_REFRESH;
                    mRefreshViewText.setText(R.string.pull_to_refresh_pull_label);
                    mRefreshViewImage.clearAnimation();
                    mRefreshViewImage.startAnimation(mReverseFlipAnimation);
                }
            }
        } else if (mCurrentScrollState == SCROLL_STATE_FLING
                && firstVisibleItem == 0
                && mRefreshState != REFRESHING) {
            mRefreshView.setVisibility(View.GONE);
            scrollListBy(mRefreshViewHeight , 1250);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        mCurrentScrollState = scrollState;
    }

    public void prepareForRefresh() {
        mRefreshView.setPadding(
                mRefreshView.getPaddingLeft(),
                mRefreshOriginalTopPadding,
                mRefreshView.getPaddingRight(),
                mRefreshView.getPaddingBottom());

        // Replace arrow with refresh drawable
        mRefreshViewImage.setImageResource(R.drawable.ic_refresh);
        // Clear any animations in the drawable and start the full rotation animation
        mRefreshViewImage.clearAnimation();
        mRefreshViewImage.startAnimation(mRotateAnimation);
        // Set refresh view text to the refreshing label
        mRefreshViewText.setText(R.string.pull_to_refresh_refreshing_label);
    }

    public void onRefresh() {
        Log.d(TAG, "onRefresh");

        if (mOnRefreshListener != null) {
            mOnRefreshListener.onRefresh();
        }
    }

    /**
     * Resets the list to a normal state after a refresh.
     */
    public void onRefreshComplete() {        
        Log.d(TAG, "onRefreshComplete");

        mRefreshView.setPadding(
                mRefreshView.getPaddingLeft(),
                mRefreshOriginalTopPadding,
                mRefreshView.getPaddingRight(),
                mRefreshView.getPaddingBottom());

        // Set refresh view text to the pull label
        mRefreshViewText.setText(R.string.pull_to_refresh_pull_label);
        // Replace refresh drawable with arrow drawable
        mRefreshViewImage.setImageResource(R.drawable.ic_pull_arrow);
        // Clear the full rotation animation
        mRefreshViewImage.clearAnimation();

        // If refresh view is visible when loading completes, scroll down to next item
        if (mRefreshView.getBottom() > 0) {
            invalidateViews();
            scrollListBy(mRefreshView.getBottom(), 750);
        }

        // Reset refresh state
        mRefreshState = PULL_TO_REFRESH;
    }

    /**
     * Invoked when the refresh view is clicked on. This is mainly used when
     * there's only a few items in the list and it's not possible to drag the
     * list.
     */
    private class OnClickRefreshListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            if (mRefreshState != REFRESHING) {
                mRefreshState = REFRESHING;
                prepareForRefresh();
                onRefresh();
            }
        }

    }

    /**
     * Interface definition for a callback to be invoked when list should be
     * refreshed.
     */
    public interface OnRefreshListener {
        /**
         * Called when the list should be refreshed.
         * <p>
         * A call to {@link PullToRefreshListView #onRefreshComplete()} is
         * expected to indicate that the refresh has completed.
         */
        public void onRefresh();
    }
}
