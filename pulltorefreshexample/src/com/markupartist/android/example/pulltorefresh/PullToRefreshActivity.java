package com.markupartist.android.example.pulltorefresh;

import java.util.Arrays;
import java.util.LinkedList;

import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.markupartist.android.widget.PullToRefreshListView;
import com.markupartist.android.widget.PullToRefreshListView.OnEndOfListReachedListener;
import com.markupartist.android.widget.PullToRefreshListView.OnRefreshListener;

public class PullToRefreshActivity extends ListActivity {    
    private ArrayAdapter<String> mAdapter;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pull_to_refresh);

        // Set a listener to be invoked when the list should be refreshed.
        PullToRefreshListView listView = (PullToRefreshListView) getListView();
        listView.setOnRefreshListener(new OnRefreshListener() {
            public void onRefresh() {
                // Do work to refresh the list here.
                new GetRobotTalkTask().execute();
            }
        });
        listView.setOnEndOfListReachedListener(new OnEndOfListReachedListener() {
            public void onEndOfListReached() {
                // Post a toast, could load more data here to extend the list
                Toast.makeText(getApplicationContext(), "End of list reached", Toast.LENGTH_SHORT).show();
            }
        });

        mAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                new LinkedList<String>(Arrays.asList(
                        getResources().getStringArray(R.array.robots))));

        setListAdapter(mAdapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        ((ArrayAdapter<String>)getListAdapter()).remove(
            (String)l.getItemAtPosition(position)
        );
    }

    private class GetRobotTalkTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            // Simulates a background job.
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                ;
            }
            return Long.toHexString(System.nanoTime());
        }

        @Override
        protected void onPostExecute(String robotTalk) {
            mAdapter.insert("Robot says: " + robotTalk, 0);
            // Call onRefreshComplete when the list has been refreshed.
            ((PullToRefreshListView) getListView()).onRefreshComplete();
        }
    }

}
