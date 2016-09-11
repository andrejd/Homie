package io.realm;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;

/**
 * Created by Andrej on 29.8.2015.
 */
public abstract class RealmRecyclerViewAdapter<T extends RealmObject, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    protected LayoutInflater inflater;
    protected RealmResults<T> realmResults;
    protected Context context;

    private final RealmChangeListener listener;

    private Realm getRealm(RealmResults<T> realmResults) {
        if( realmResults == null ) return null;
        return (Realm) realmResults.realm;
    }

    public RealmRecyclerViewAdapter(Context context, RealmResults<T> realmResults, boolean automaticUpdate) {
        if(context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        this.context = context;
        this.realmResults = realmResults;
        this.inflater = LayoutInflater.from(context);
        this.listener = (!automaticUpdate) ? null : new RealmChangeListener() {
            @Override
            public void onChange() {
                Log.i("REALM","notifier about update");
                notifyDataSetChanged();
            }
        };

        if(listener != null && realmResults != null) {
            Realm rlm = getRealm(realmResults);
            if(rlm != null)
            {
                rlm.removeChangeListener(listener);
                rlm.addChangeListener(listener);
            }
        }
    }

    @Override
    public long getItemId(int i) {
        // TODO: find better solution once we have unique IDs
        return i;
    }

    public T getItem(int i) {
        if(realmResults == null) {
            return null;
        }
        return realmResults.get(i);
    }

    public void updateRealmResults(RealmResults<T> queryResults) {
        if(listener != null) {
            // Making sure that Adapter is refreshed correctly if new RealmResults come from another Realm
            if(this.realmResults != null) {
                getRealm(this.realmResults).removeChangeListener(listener);
            }
            if(queryResults != null) {
                getRealm(queryResults).addChangeListener(listener);
            }
        }

        this.realmResults = queryResults;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        try{
            if(realmResults == null) {
                return 0;
            }
            return realmResults.size();
        }
        catch (Exception ex)
        {
            Log.e(this.getClass().getSimpleName(),ex.toString());
        }
        return 0;

    }
}
