# Pull To Refresh for Android

This project aims to provide a reusable pull to refresh widget for Android.

**Note:** This widget requires API Level 8 (Android 2.2).

## Demo

<object classid='clsid:d27cdb6e-ae6d-11cf-96b8-444553540000' codebase='http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=9,0,115,0' width='560' height='345'><param name='movie' value='http://screenr.com/Content/assets/screenr_1116090935.swf' /><param name='flashvars' value='i=178819' /><param name='allowFullScreen' value='true' /><embed src='http://screenr.com/Content/assets/screenr_1116090935.swf' flashvars='i=178819' allowFullScreen='true' width='560' height='345' pluginspage='http://www.macromedia.com/go/getflashplayer'></embed></object>

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

* Jason Knight <http://www.synthable.com/>
* Eddie Ringle <http://eddieringle.com/>

## License
Copyright (c) 2011 [Johan Nilsson](http://markupartist.com)

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)


