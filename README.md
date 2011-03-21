# Pull To Refresh for Android

This project aims to provide a reusable pull to refresh widget for Android.

![Screenshot](https://github.com/johannilsson/android-pulltorefresh/raw/master/android-pull-to-refresh.png)

**Note:** This widget requires API Level 8 (Android 2.2).

Repository at <https://github.com/johannilsson/android-pulltorefresh>.

## Usage

### Layout

    <!--
    The PullToRefreshListView replaces a standard ListView widget.
    -->
    <com.markupartist.android.widget.PullToRefreshListView
        android:id="@+id/android:list"
        android:layout_height="fill_parent"
        android:layout_width="fill_parent"
        />

### Activity

    // Set a listener to be invoked when the list should be refreshed.
    ((PullToRefreshListView) getListView()).setOnRefreshListener(new OnRefreshListener() {
        @Override
        public void onRefresh() {
            // Do work to refresh the list here.
            new GetDataTask().execute();
        }
    });

    private class GetDataTask extends AsyncTask<Void, Void, String[]> {
        ...
        @Override
        protected void onPostExecute(String[] result) {
            mListItems.addFirst("Added after refresh...");
            // Call onRefreshComplete when the list has been refreshed.
            ((PullToRefreshListView) getListView()).onRefreshComplete();
            super.onPostExecute(result);
        }
    }

## Contributors

* [Jason Knight](http://www.synthable.com/) - https://github.com/synthable
* [Eddie Ringle](http://eddieringle.com/) - https://github.com/eddieringle
* [Christof Dorner](http://chdorner.com) - https://github.com/chdorner

## License
Copyright (c) 2011 [Johan Nilsson](http://markupartist.com)

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)


