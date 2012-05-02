package com.markupartist.android.widget;


import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

import com.markupartist.android.widget.pulltorefresh.R;

public class PullToRefreshListView extends ListView implements OnScrollListener {

    private static final int PULL_TO_REFRESH = 2;
    private static final int RELEASE_TO_REFRESH = 3;
    private static final int REFRESHING = 4;

    private int mRefreshState = PULL_TO_REFRESH;

    private OnRefreshListener mOnRefreshListener;
    private OnEndOfListReachedListener mOnEndOfListListener;

    /**
     * Listener that will receive notifications every time the list scrolls.
     */
    private OnScrollListener mOnScrollListener;

    private RelativeLayout mRefreshView;
    private TextView mRefreshViewText;
    private ImageView mRefreshViewImage;
    private ProgressBar mRefreshViewProgress;
    private TextView mRefreshViewLastUpdated;

    private int mCurrentScrollState;

    private RotateAnimation mFlipAnimation;
    private RotateAnimation mReverseFlipAnimation;

    private int mRefreshViewHeight;
    private int mRefreshOriginalTopPadding;
    private int mLastMotionY;
    private int mHeight = -1;
    private int mScrollPriorLast = -1;

    private boolean mBounceHack;
    private TextView mFooterView;

    private DataSetObserver mDataSetObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            adaptFooterHeight();
        }
    };

    public PullToRefreshListView(Context context) {
        super(context);
        init(context);
    }

    public PullToRefreshListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PullToRefreshListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        setupAnimations();

        setupViews(context);

        super.setOnScrollListener(this);
    }

    private void setupViews(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        // Refresh view
		mRefreshView = (RelativeLayout) inflater.inflate(
				R.layout.pull_to_refresh_header, this, false);
		mRefreshView.setOnClickListener(new OnClickRefreshListener());

		// The refresh view text label
		mRefreshViewText =
            (TextView) mRefreshView.findViewById(R.id.pull_to_refresh_text);

		// The arrow
		mRefreshViewImage =
            (ImageView) mRefreshView.findViewById(R.id.pull_to_refresh_image);
        mRefreshViewImage.setMinimumHeight(50);

        // The progress spinner
        mRefreshViewProgress =
            (ProgressBar) mRefreshView.findViewById(R.id.pull_to_refresh_progress);

        // The refresh view subtitle label.
        mRefreshViewLastUpdated =
            (TextView) mRefreshView.findViewById(R.id.pull_to_refresh_updated_at);

        mFooterView = new TextView(context);
        mFooterView.setText(" ");
        mFooterView.setHeight(0);
        addFooterView(mFooterView, null, false);

        mRefreshOriginalTopPadding = mRefreshView.getPaddingTop();
        addHeaderView(mRefreshView);

        measureView(mRefreshView);
        mRefreshViewHeight = mRefreshView.getMeasuredHeight();
    }

    private void setupAnimations() {
        // Load all of the animations we need in code rather than through XML
        mFlipAnimation = new RotateAnimation(0, -180,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mFlipAnimation.setInterpolator(new LinearInterpolator());
        mFlipAnimation.setDuration(250);
        mFlipAnimation.setFillAfter(true);
        mReverseFlipAnimation = new RotateAnimation(-180, 0,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mReverseFlipAnimation.setInterpolator(new LinearInterpolator());
        mReverseFlipAnimation.setDuration(250);
        mReverseFlipAnimation.setFillAfter(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mHeight == -1) {  // do it only once
            mHeight = getHeight(); // getHeight only returns useful data after first onDraw()
            adaptFooterHeight();
        }
    }
    
    /**
     * Sets list view selection to first element in adapter unless refreshing where it will set it to the refresh view element
     */
	private void setSelectionToFirst() {
		if (getAdapter() != null && mRefreshState != REFRESHING) {
			// This allows for the divider to push the selection down from the top so the fading edge doesn't obscure the first view
			setSelectionFromTop(1, getDividerHeight());
		} else {
			// Refreshing or no adapter, display the refresh view
			super.setSelection(0);
		}
	}
	
	@Override
	public void setSelection(int position) {
		// If the 0th or 1st element do special behavior
		if (position <= 1) {
			setSelectionToFirst();
		} else {
			// Force to index 0 while refreshing, allow other indices while not
			super.setSelection((mRefreshState == REFRESHING) ? 0 : position);
		}
	}

	/**
	 * Adapts the height of the footer view.
	 */
	private void adaptFooterHeight() {
		if (mHeight == -1) {
			return;
		}
		// We can fill up to the total height - header padding to get it off screen
		int spaceToFill = mHeight - mRefreshOriginalTopPadding;
		int itemHeight = getTotalItemHeight(spaceToFill);
		spaceToFill -= itemHeight;

		if (spaceToFill <= 0) {
			mFooterView.setHeight(0);
		} else {
			mFooterView.setHeight(spaceToFill);
			setSelectionToFirst();
		}
	}

	/**
	 * Calculates the combined height of all items in the adapter. Does not look at header and footer
	 * 
	 * Modified from
	 * http://iserveandroid.blogspot.com/2011/06/how-to-calculate-lsitviews-total.html
	 * 
	 * @param spaceToFill
	 *            the maximum item height we care about
	 * @return total item height, capped to spaceToFill
	 */
	private int getTotalItemHeight(int spaceToFill) {
		ListAdapter adapter = getAdapter();
		// If no adapter there is no item height
		if (adapter == null) {
			return 0;
		}
		int listviewElementsheight = 0;
		// Need to constrain width for lists with variable height items or items with wrapping text
		int desiredWidth = MeasureSpec.makeMeasureSpec(getWidth(),
				MeasureSpec.AT_MOST);
		// Skip header and footer
		for (int i = 1; i < adapter.getCount() - 1
				&& listviewElementsheight < spaceToFill; i++) {
			View mView = adapter.getView(i, null, this);
			mView.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
			listviewElementsheight += mView.getMeasuredHeight();
		}
		// Add in dividers
		listviewElementsheight += getDividerHeight() * (adapter.getCount() - 1);
		if (listviewElementsheight > spaceToFill) {
			listviewElementsheight = spaceToFill;
		}
		return listviewElementsheight;
	}

    @Override
    protected void onAttachedToWindow() {
    	setSelectionToFirst();
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        if (adapter != null) {
            adapter.registerDataSetObserver(mDataSetObserver);
        }

        super.setAdapter(adapter);
        // Ensure with new data the view is reinitialized to beginning
        setSelectionToFirst();
        // With different data we may need to adjust footer height
		adaptFooterHeight();
		mScrollPriorLast = -1;
    }

    /**
     * Set the listener that will receive notifications every time the list
     * scrolls.
     * 
     * @param l The scroll listener. 
     */
    @Override
    public void setOnScrollListener(AbsListView.OnScrollListener l) {
        mOnScrollListener = l;
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
     * Register a callback to be invoked when the end of the list is reached.
     * 
     * @param onEndOfListListener The callback to run.
     */
    public void setOnEndOfListReachedListener(OnEndOfListReachedListener onEndOfListListener) {
    	mOnEndOfListListener = onEndOfListListener;
    }

    /**
     * Set a text to represent when the list was last updated. 
     * @param lastUpdated Last updated at.
     */
    public void setLastUpdated(CharSequence lastUpdated) {
        if (lastUpdated != null) {
            mRefreshViewLastUpdated.setVisibility(View.VISIBLE);
            mRefreshViewLastUpdated.setText(lastUpdated);
        } else {
            mRefreshViewLastUpdated.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int y = (int) event.getY();
        mBounceHack = false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                if (!isVerticalScrollBarEnabled()) {
                    setVerticalScrollBarEnabled(true);
                }
                if (getFirstVisiblePosition() == 0 && mRefreshState != REFRESHING) {
                    if ((mRefreshView.getBottom() >= mRefreshViewHeight
                            || mRefreshView.getTop() >= 0)
                            && mRefreshState == RELEASE_TO_REFRESH) {
                        // Initiate the refresh
                        prepareForRefresh();
                        onRefresh();
                    } else if (mRefreshView.getBottom() < mRefreshViewHeight
                            || mRefreshView.getTop() <= 0) {
                        // Abort refresh and scroll down below the refresh view
                        resetHeader();
                        setSelectionToFirst();
                    }
                }
                break;
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                applyHeaderPadding(event);
                break;
        }
        return super.onTouchEvent(event);
    }

    private void applyHeaderPadding(MotionEvent ev) {
        // getHistorySize has been available since API 1
        int pointerCount = ev.getHistorySize();

        for (int p = 0; p < pointerCount; p++) {
            if (mRefreshState == RELEASE_TO_REFRESH) {
                if (isVerticalFadingEdgeEnabled()) {
                    setVerticalScrollBarEnabled(false);
                }

                int historicalY = (int) ev.getHistoricalY(p);

                // Calculate the padding to apply, we divide by 1.7 to
                // simulate a more resistant effect during pull.
                int topPadding = (int) (((historicalY - mLastMotionY)
                        - mRefreshViewHeight) / 1.7);

                mRefreshView.setPadding(
                        mRefreshView.getPaddingLeft(),
                        topPadding,
                        mRefreshView.getPaddingRight(),
                        mRefreshView.getPaddingBottom());
            }
        }
    }

    /**
     * Sets the header padding back to original size.
     */
    private void resetHeaderPadding() {
        mRefreshView.setPadding(
                mRefreshView.getPaddingLeft(),
                mRefreshOriginalTopPadding,
                mRefreshView.getPaddingRight(),
                mRefreshView.getPaddingBottom());
    }

    /**
     * Resets the header to the original state.
     */
    private void resetHeader() {
        mRefreshState = PULL_TO_REFRESH;

        resetHeaderPadding();

        // Set refresh view text to the pull label
        mRefreshViewText.setText(R.string.pull_to_refresh_pull_label);
        // Replace refresh drawable with arrow drawable
        mRefreshViewImage.setImageResource(R.drawable.ic_pulltorefresh_arrow);
        // Clear the full rotation animation
        mRefreshViewImage.clearAnimation();
        // Hide progress bar and arrow.
        //mRefreshViewImage.setVisibility(View.GONE);
        mRefreshViewProgress.setVisibility(View.GONE);
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

    public void onScroll(AbsListView view, int firstVisibleItem,
            int visibleItemCount, int totalItemCount) {
        // When the refresh view is completely visible, change the text to say
        // "Release to refresh..." and flip the arrow drawable.
        if (mCurrentScrollState == SCROLL_STATE_TOUCH_SCROLL
                && mRefreshState != REFRESHING) {
            if (firstVisibleItem == 0) {
                mRefreshViewImage.setVisibility(View.VISIBLE);
                if ((mRefreshView.getBottom() >= mRefreshViewHeight + 20
                        || mRefreshView.getTop() >= 0)
                        && mRefreshState != RELEASE_TO_REFRESH) {
                    mRefreshViewText.setText(R.string.pull_to_refresh_release_label);
                    mRefreshViewImage.clearAnimation();
                    mRefreshViewImage.startAnimation(mFlipAnimation);
                    mRefreshState = RELEASE_TO_REFRESH;
                } else if (mRefreshView.getBottom() < mRefreshViewHeight + 20
                        && mRefreshState != PULL_TO_REFRESH) {
                    mRefreshViewText.setText(R.string.pull_to_refresh_pull_label);
                    mRefreshViewImage.clearAnimation();
                    mRefreshViewImage.startAnimation(mReverseFlipAnimation);
                    mRefreshState = PULL_TO_REFRESH;
                }
            } else {
                mRefreshViewImage.setVisibility(View.GONE);
                resetHeader();
            }
        } else if (mCurrentScrollState == SCROLL_STATE_FLING
                && firstVisibleItem == 0
                && mRefreshState != REFRESHING) {
        	setSelectionToFirst();
            mBounceHack = true;
        } else if (mBounceHack && mCurrentScrollState == SCROLL_STATE_FLING) {
        	setSelectionToFirst();
        }
        
        if(mOnEndOfListListener != null)
        {
        	// what is the bottom item that is visible
        	int lastInScreen = firstVisibleItem + visibleItemCount;

        	// is the bottom item visible
        	if (lastInScreen == totalItemCount) {
        		// Only do callback 1x when we reach the bottom
        		if (mScrollPriorLast != lastInScreen) {
        			mScrollPriorLast = lastInScreen;
        			// Do end of list reached callback
        			mOnEndOfListListener.onEndOfListReached();
        		}
        	}
        }

        if (mOnScrollListener != null) {
            mOnScrollListener.onScroll(view, firstVisibleItem,
                    visibleItemCount, totalItemCount);
        }
    }

    public void onScrollStateChanged(AbsListView view, int scrollState) {
        mCurrentScrollState = scrollState;

        if (mCurrentScrollState == SCROLL_STATE_IDLE) {
            mBounceHack = false;
        }

        if (mOnScrollListener != null) {
            mOnScrollListener.onScrollStateChanged(view, scrollState);
        }
    }

    public void prepareForRefresh() {
        resetHeaderPadding();

        mRefreshViewImage.setVisibility(View.GONE);
        // We need this hack, otherwise it will keep the previous drawable.
        mRefreshViewImage.setImageDrawable(null);
        mRefreshViewProgress.setVisibility(View.VISIBLE);

        // Set refresh view text to the refreshing label
        mRefreshViewText.setText(R.string.pull_to_refresh_refreshing_label);

        mRefreshState = REFRESHING;
    }

    public void onRefresh() {
        if (mOnRefreshListener != null) {
            mOnRefreshListener.onRefresh();
        }
    }

    /**
     * Resets the list to a normal state after a refresh.
     * @param lastUpdated Last updated at.
     */
    public void onRefreshComplete(CharSequence lastUpdated) {
        setLastUpdated(lastUpdated);
        onRefreshComplete();
    }

    /**
     * Resets the list to a normal state after a refresh.
     */
    public void onRefreshComplete() {        
        resetHeader();

        // If refresh view is visible when loading completes, scroll down to
        // the next item.
        if (mRefreshView.getBottom() > 0) {
            invalidateViews();
            setSelectionToFirst();
        }
    }

    /**
     * Invoked when the refresh view is clicked on. This is mainly used when
     * there's only a few items in the list and it's not possible to drag the
     * list.
     */
    private class OnClickRefreshListener implements OnClickListener {
        public void onClick(View v) {
            if (mRefreshState != REFRESHING) {
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
    
    /**
     * Interface definition for a callback to be invoked when end of list is reached.
     */
    public interface OnEndOfListReachedListener {
        /**
         * Called 1x when the end of list is reached. This might be used to implement an endless list which auto-loads more data as users scroll.
         * It will only be called again if the adapter changes or the list grows/shrinks
         */
        public void onEndOfListReached();
    }
}
