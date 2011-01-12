package com.markupartist.android.widget;


import com.markupartist.android.widget.pulltorefresh.R;

import android.content.Context;
import android.content.res.Resources;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

public class PullToRefreshListView extends ListView implements OnScrollListener {

    private static final String TAG = "PullToRefreshListView";
    private LayoutInflater mInflater;
    private LinearLayout mRefreshView;
    private int mCurrentScrollState;
    private int mRefreshViewHeight;
    private int mPullBounce;
    private boolean mRefreshing;

    public PullToRefreshListView(Context context, AttributeSet attrs) {
        super(context, attrs);

        final Resources res = context.getResources();
        mPullBounce = res.getDimensionPixelSize(R.dimen.pull_to_refresh_bounce);

        mInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        mRefreshView = (LinearLayout) mInflater.inflate(
                R.layout.pull_to_refresh_header, null);

        addHeaderView(mRefreshView);

        setOnScrollListener(this);

        measureView(mRefreshView);
        mRefreshViewHeight = mRefreshView.getMeasuredHeight();

        smoothScrollBy(mRefreshViewHeight, 0);
        //scrollBy(0, mRefreshViewHeight);

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    //Log.d(TAG, "BLA: " + getFirstVisiblePosition());

                    //final int top = mRefreshView.getTop();
                    final int top = getFirstVisiblePosition();
                    Log.d(TAG, "top: " + getFirstVisiblePosition());
                    if (top == 0 && !mRefreshing/* || top >= -30*/) {
                        //Log.d(TAG, "Should refresh?");
                        onRefresh();
                        //return true;
                    }
                }
                return false;
            }
        });
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
        if (firstVisibleItem == 0 && mCurrentScrollState == SCROLL_STATE_FLING) {
            smoothScrollBy(mRefreshViewHeight, 1000);
            //scrollBy(0, mRefreshViewHeight);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        mCurrentScrollState = scrollState;
    }

    public void onRefresh() {
        Log.d(TAG, "onRefresh");

        final int top = mRefreshView.getTop();
        if (top < -30) {
            Log.d(TAG, "Backing off refresh...");
            smoothScrollBy(mRefreshViewHeight + top, 1000);
            return;
        }

        invalidate();
        smoothScrollBy(mRefreshView.getTop() + mPullBounce, 500);
        //scrollBy(0, mPullBounce);

        mRefreshing = true;

        Animation rotateAnimation =
            AnimationUtils.loadAnimation(getContext(),
                    R.anim.pull_to_refresh_anim);
        rotateAnimation.setRepeatCount(Animation.INFINITE);

        final TextView text = (TextView) mRefreshView.findViewById(R.id.pull_to_refresh_text);
        final ImageView staticSpinner = (ImageView) mRefreshView.findViewById(R.id.pull_to_refresh_static_spinner);

        text.setText(getContext().getText(R.string.pull_to_refresh_loading_label));
        staticSpinner.startAnimation(rotateAnimation);

        // TODO: Temporary, to fake some network work or similar. Replace with callback.
        new CountDownTimer(4000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }
            @Override
            public void onFinish() {
                onRefreshComplete();
            }
        }.start();
    }

    public void onRefreshComplete() {        
        Log.d(TAG, "onRefreshComplete");
        final TextView text = (TextView) mRefreshView.findViewById(R.id.pull_to_refresh_text);
        final ImageView staticSpinner = (ImageView) mRefreshView.findViewById(R.id.pull_to_refresh_static_spinner);

        text.setText(getContext().getText(R.string.pull_to_refresh_label));
        staticSpinner.setAnimation(null);

        final int top = mRefreshView.getTop();
        Log.d(TAG, "Refresh top: " + top);
        if (top == 0 || top >= -mPullBounce) {
            invalidateViews();
            //invalidate();
            int scrollDistance = mRefreshViewHeight - mPullBounce;
            smoothScrollBy(scrollDistance, 1000);
            //scrollBy(0, scrollDistance);
        }
        mRefreshing = false;
    }
}
