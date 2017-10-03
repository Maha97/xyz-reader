package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.palette.PaletteTransformation;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, AppBarLayout.OnOffsetChangedListener {

    public static final String ARG_ITEM_ID = "item_id";
    private static final String TAG = "ArticleDetailFragment";
    //new
    private static final int PERCENTAGE_TO_SHOW_IMAGE = 20;
    Toolbar toolbar;
    Typeface mTypeface;
    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private View mFab;
    private int mMaxScrollSize;
    private boolean mIsImageHidden;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private ImageView mPhotoView;
    private TextView body_text;
    private TextView article_by_line;
    private boolean mIsCard = false;


    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        mTypeface = Typeface.createFromAsset(getActivity().getAssets(), "Rosario-Regular.ttf");
        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_detail_article_new, container, false);
        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);
        collapsingToolbarLayout =
                (CollapsingToolbarLayout) mRootView.findViewById(R.id.collapse_bar);

        article_by_line = (TextView) mRootView.findViewById(R.id.article_byline);

        body_text = (TextView) mRootView.findViewById(R.id.article_body);

        toolbar = (Toolbar) mRootView.findViewById(R.id.toolbar);


        //new
        mFab = mRootView.findViewById(R.id.share_fab);
        AppBarLayout appbar = (AppBarLayout) mRootView.findViewById(R.id.flexible_example_appbar);
        appbar.addOnOffsetChangedListener(this);

        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });


        return mRootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        body_text.setTypeface(mTypeface);
        article_by_line.setTypeface(mTypeface);

    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        if (mCursor != null) {
            mRootView.setVisibility(View.VISIBLE);
            article_by_line.setMovementMethod(new LinkMovementMethod());
            collapsingToolbarLayout.setTitle(mCursor.getString(ArticleLoader.Query.TITLE));
            article_by_line.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by <font color='#00796B'><b>"
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)
                            + "</b></font>"));
            body_text.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)));

            final PaletteTransformation paletteTransformation = PaletteTransformation.instance();

            Picasso.with(getActivity())
                    .load(mCursor.getString(ArticleLoader.Query.PHOTO_URL))
                    .transform(paletteTransformation)
                    .into(mPhotoView, new Callback.EmptyCallback() {
                        @Override
                        public void onSuccess() {

                            if (mPhotoView != null) {
                                Bitmap bitmap = ((BitmapDrawable) mPhotoView.getDrawable()).getBitmap(); // Ew!
                                Palette palette = PaletteTransformation.getPalette(bitmap);
                                if (palette != null) {
                                    applyPalleteToWindow(palette);
                                }

                            }

                        }
                    });

        } else {
            mRootView.setVisibility(View.GONE);
            collapsingToolbarLayout.setTitle("N/A");
            article_by_line.setText("N/A");
            body_text.setText("N/A");
        }
    }


    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        if (isAdded())
            bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;

        bindViews();
    }


    //Apply toolbar status and navigation color from palatte
    private void applyPalleteToWindow(Palette palette) {


        //Default colors for window
        int colorPrimary = getResources().getColor(R.color.theme_primary);
        int colorPrimaryDark = getResources().getColor(R.color.theme_primary_dark);


        if (palette.getDarkMutedSwatch() != null) {

            colorPrimaryDark = palette.getDarkMutedSwatch().getRgb();

            float[] hsv = new float[3];
            Color.colorToHSV(colorPrimaryDark, hsv);
            hsv[2] *= 1.5f;
            colorPrimary = Color.HSVToColor(hsv);
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            Window window = getActivity().getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(colorPrimaryDark);
            window.setNavigationBarColor(colorPrimaryDark);

            collapsingToolbarLayout.setContentScrimColor(colorPrimary);

        }
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        if (mMaxScrollSize == 0)
            mMaxScrollSize = appBarLayout.getTotalScrollRange();

        int currentScrollPercentage = (Math.abs(i)) * 100
                / mMaxScrollSize;

        if (currentScrollPercentage >= PERCENTAGE_TO_SHOW_IMAGE) {
            if (!mIsImageHidden) {
                mIsImageHidden = true;

                ViewCompat.animate(mFab).scaleY(0).scaleX(0).start();
            }
        }

        if (currentScrollPercentage < PERCENTAGE_TO_SHOW_IMAGE) {
            if (mIsImageHidden) {
                mIsImageHidden = false;
                ViewCompat.animate(mFab).scaleY(1).scaleX(1).start();
            }
        }
    }


}