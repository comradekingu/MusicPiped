package deep.ryd.rydplayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;


import org.schabi.newpipe.extractor.Info;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.InfoItemsCollector;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.search.InfoItemsSearchCollector;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.search.SearchInfo;
import org.schabi.newpipe.extractor.services.youtube.YoutubeService;
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeSearchExtractor;
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory;
import org.schabi.newpipe.extractor.stream.Stream;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    Activity self;
    EditText searchText;
    RecyclerView resultRecycler;
    RecyclerView.Adapter mAdapter;
    RecyclerView.LayoutManager mLayoutManager;
    List<InfoItem> searchItems = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        self=this;

        searchText=findViewById(R.id.searchBar);
        ImageButton searchSubmit=findViewById(R.id.searchSubmit);
        resultRecycler = findViewById(R.id.resultRecycler);

        resultRecycler.setHasFixedSize(true);

        mLayoutManager=new LinearLayoutManager(this);
        resultRecycler.setLayoutManager(mLayoutManager);

        mAdapter=new MyAdapter(searchItems,this);
        resultRecycler.setAdapter(mAdapter);

        searchSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String st=searchText.getText().toString();
                new Extractor(resultRecycler,searchItems,self,(ProgressBar)findViewById(R.id.loadingCircle)).execute(st);
            }
        });
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent( event );
    }


}
class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder>{

    String searchq;
    List<InfoItem> infoItems;
    Activity activity;

    public MyAdapter(List<InfoItem> infoItems,Activity activity){
        this.infoItems = infoItems;
        this.activity=activity;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        CardView view= new CardView(viewGroup.getContext());
        ViewGroup.MarginLayoutParams params=new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(10,10,10,10);
        view.setLayoutParams(params);
        view.setPadding(10,10,10,10);

        view.addView((ConstraintLayout) LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.resultcard,viewGroup,false));

        MyViewHolder vh=new MyViewHolder(view);


        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, int i) {

        myViewHolder.streamInfoItem=(StreamInfoItem)infoItems.get(i);
        CardView cardView=myViewHolder.cardView;
        ConstraintLayout constraintLayout=(ConstraintLayout)cardView.getChildAt(0);
        ImageView img=(ImageView)constraintLayout.findViewById(R.id.cardThumb);
        TextView title=(TextView)constraintLayout.findViewById(R.id.cardTitle);

        title.setText(infoItems.get(i).getName());
        //title.setText("TEST CARD "+new Integer(i).toString());
        //t.setText(new Integer(i).toString());
        new setThumbCard().execute(myViewHolder);
    }

    @Override
    public int getItemCount() {
        return infoItems.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{
        public StreamInfoItem streamInfoItem;
        public CardView cardView;
        public  MyViewHolder(CardView cardView){
            super(cardView);
            this.cardView=cardView;
            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent result = new Intent();
                    result.putExtra("newurl",streamInfoItem.getUrl());
                    Activity activity=(Activity)v.getContext();
                    activity.setResult(Activity.RESULT_OK,result);
                    activity.finish();
                }
            });

        }
    }
}

class Extractor extends AsyncTask<String,Integer,Integer>{


    RecyclerView mRecycler;
    List<InfoItem> infoItems;
    Activity context;
    ProgressBar loader;

    public Extractor(RecyclerView mRecycler,List<InfoItem> infoItems,Activity context,ProgressBar loader){
        this.mRecycler=mRecycler;
        this.infoItems=infoItems;
        this.context=context;
        this.loader=loader;
    }

    @Override
    protected Integer doInBackground(String... strings) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loader.setVisibility(View.VISIBLE);
            }
        });
        String st=strings[0];
        Downloader.init(null);
        NewPipe.init(Downloader.getInstance());
        int serviceID=NewPipe.getIdOfService("YouTube");
        try {
            List<String> contentFilter = new ArrayList<>();
            contentFilter.add("videos");
            YoutubeService ys =(YoutubeService)NewPipe.getService(serviceID);
            YoutubeSearchExtractor ySE=(YoutubeSearchExtractor)ys.getSearchExtractor(st,contentFilter,null,"IN");
            ySE.onFetchPage(Downloader.getInstance());
            ListExtractor.InfoItemsPage<InfoItem> ife= ySE.getInitialPage();
            List<InfoItem> infoItemsList = ife.getItems();
            Log.i("ryd",infoItemsList.toString());

            infoItems.clear();
            for(int i=0;i<infoItemsList.size();i++){
                infoItems.add(infoItemsList.get(i));
            }
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loader.setVisibility(View.INVISIBLE);
                    mRecycler.getAdapter().notifyDataSetChanged();
                }
            });

        } catch (ExtractionException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
class setThumbCard extends AsyncTask<MyAdapter.MyViewHolder,Integer,Integer>{

    @Override
    protected Integer doInBackground(MyAdapter.MyViewHolder... viewHolders) {
        try {
            URL urlConnection = new URL(viewHolders[0].streamInfoItem.getThumbnailUrl());
            Log.i("rypd","Thumbnail URL downloading "+urlConnection.toString());
            HttpURLConnection connection = (HttpURLConnection) urlConnection
                    .openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            final Bitmap myBitmap = BitmapFactory.decodeStream(input);
            ConstraintLayout constraintLayout = (ConstraintLayout)viewHolders[0].cardView.getChildAt(0);
            final ImageView img=(ImageView)constraintLayout.findViewById(R.id.cardThumb);
            ((Activity)img.getContext()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    img.setImageBitmap(myBitmap);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}


