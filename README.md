# Pull To Refresh for Android

This project aims to provide a reusable pull to refresh widget for Android.

![Screenshot](https://github.com/johannilsson/android-pulltorefresh/raw/master/android-pull-to-refresh.png)

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

## 1.5 Support

To use the widget on 1.5 the necessary drawables needs to be copied to that
projects drawable folder. The drawables needed by the widget can be found in
the drawable-hdpi folder in the library project. For an example of this look
at the example project.

## Contributors

* [Jason Knight](http://www.synthable.com/) - https://github.com/synthable
* [Eddie Ringle](http://eddieringle.com/) - https://github.com/eddieringle
* [Christof Dorner](http://chdorner.com) - https://github.com/chdorner

## License
Copyright (c) 2011 [Johan Nilsson](http://markupartist.com)

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)


