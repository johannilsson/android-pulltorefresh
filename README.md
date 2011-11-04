# Pull To Refresh for Android

This project aims to provide a reusable pull to refresh widget for Android.

![Screenshot](https://github.com/johannilsson/android-pulltorefresh/raw/master/android-pull-to-refresh.png)

Repository at <https://github.com/johannilsson/android-pulltorefresh>.

## Usage

### Layout

``` xml
<!--
  The PullToRefreshListView replaces a standard ListView widget.
-->
<com.markupartist.android.widget.PullToRefreshListView
    android:id="@+id/android:list"
    android:layout_height="fill_parent"
    android:layout_width="fill_parent"
    />
```

### Activity

``` java
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
```

### Last Updated

It's possible to add a last updated time using the method `setLastUpdated`
and `onRefreshComplete`. The text provided to these methods will be set below
the Release to refresh text. Note that the time representation is not validated
replaces the previous text, which means that it's possible and recommended to
add a text similar to "Last Update: 15:23". This might be changed in future
versions.

## 1.5 Support

To use the widget on 1.5 the necessary drawables needs to be copied to that
projects drawable folder. The drawables needed by the widget can be found in
the drawable-hdpi folder in the library project.

## Contributors

* [Jason Knight](http://www.synthable.com/) - <https://github.com/synthable>
* [Eddie Ringle](http://eddieringle.com/) - <https://github.com/eddieringle>
* [Christof Dorner](http://chdorner.com) - <https://github.com/chdorner>
* [Olof Brickarp](http://www.yay.se) - <https://github.com/coolof>
* [James Smith](http://loopj.com/) - <https://github.com/loopj>
* [Alex Volovoy](http://bytesharp.com/) - <https://github.com/avolovoy>
* Bo Maryniuk
* [kidfolk](https://github.com/kidfolk)
* [Tim Mahoney](https://github.com/timahoney)
* [Richard Guest](https://github.com/quiffman)

## Are you using this widget?

If you are using this widget please feel free to add your app to the
[wiki](https://github.com/johannilsson/android-pulltorefresh/wiki/Apps).

## License
Copyright (c) 2011 [Johan Nilsson](http://markupartist.com)

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)


