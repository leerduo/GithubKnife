package com.quinn.githubknife.ui.activity;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.quinn.githubknife.R;
import com.quinn.githubknife.presenter.AuthPresenter;
import com.quinn.githubknife.presenter.AuthPresenterImpl;
import com.quinn.githubknife.ui.BaseActivity;
import com.quinn.githubknife.ui.fragments.ReceivedEventFragment;
import com.quinn.githubknife.ui.fragments.FollowerFragment;
import com.quinn.githubknife.ui.fragments.FollowingFragment;
import com.quinn.githubknife.ui.fragments.StarredRepoFragment;
import com.quinn.githubknife.ui.fragments.UserRepoFragment;
import com.quinn.githubknife.view.MainAuthView;
import com.quinn.githubknife.utils.PreferenceUtils;
import com.quinn.httpknife.github.User;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends BaseActivity implements MainAuthView,NavigationView.OnNavigationItemSelectedListener {

    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    @Bind(R.id.nav_view)
    NavigationView navigationVIew;
    @Bind(R.id.viewpager)
    ViewPager viewpager;
    @Bind(R.id.tabs)
    TabLayout tab;

    private TextView txt_user;
    private CircleImageView img_avatar;

    private Adapter adapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private ImageLoader imageLoader;
    private ImageLoadingListener animateFirstListener = new AnimateFirstDisplayListener();
    private DisplayImageOptions option;
    private AuthPresenter presenter;
    private String loginUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        img_avatar = (CircleImageView) navigationVIew.findViewById(R.id.avatar);
        txt_user = (TextView)navigationVIew.findViewById(R.id.headerText);
        imageLoader = ImageLoader.getInstance();
        option = new DisplayImageOptions.Builder().cacheInMemory(true).cacheOnDisk(true)
                .considerExifParams(true).build();
        toolbar.setTitle("Github");
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true); //设置返回键可用
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,
                mDrawerLayout, toolbar, R.string.app_name,
                R.string.app_name);
        toggle.syncState();
        mDrawerLayout.setDrawerListener(toggle);
        navigationVIew.setNavigationItemSelectedListener(this);
        adapter = new Adapter(getSupportFragmentManager());
        viewpager.setAdapter(adapter);
        presenter = new AuthPresenterImpl(this,this);
        presenter.auth();

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        menuItem.setChecked(true);
        mDrawerLayout.closeDrawers();
        setUpTab(menuItem.getItemId());
        return true;
    }

    public void setUpTab(int id){
        adapter.clear();
        switch (id){
            case R.id.nav_home:
                viewpager.setOffscreenPageLimit(3);
                adapter.addFragment(ReceivedEventFragment.getInstance(loginUser), "Events");
                adapter.addFragment(UserRepoFragment.getInstance(loginUser),"Repository");
                adapter.addFragment(StarredRepoFragment.getInstance(loginUser),"Starred");
                break;
            case R.id.nav_friends:
                viewpager.setOffscreenPageLimit(2);
                adapter.addFragment(FollowerFragment.getInstance(loginUser),"Follower");
                adapter.addFragment(FollowingFragment.getInstance(loginUser),"Following");
                break;
            case R.id.nav_gist:
                break;
            case R.id.nav_setting:
                break;
            case R.id.nav_about:
                break;
        }
        adapter.notifyDataSetChanged();
        tab.setupWithViewPager(viewpager);

    }



    @Override
    public void doneAuth(User user) {
        String avatar = "";
        if(user != null && user.getAvatar_url() != null && !user.getAvatar_url().isEmpty())
            avatar = user.getAvatar_url();
        if(avatar.isEmpty() == false)
            PreferenceUtils.putString(this, PreferenceUtils.Key.AVATAR,avatar);
        else{
            avatar = PreferenceUtils.getString(this, PreferenceUtils.Key.AVATAR);
        }
        loginUser = PreferenceUtils.getString(this, PreferenceUtils.Key.ACCOUNT);
        txt_user.setText(loginUser);
        imageLoader.displayImage(avatar,img_avatar,option,animateFirstListener);
        tab.setupWithViewPager(viewpager);
        setUpTab(R.id.nav_home);
    }




    static class Adapter extends FragmentStatePagerAdapter {

        private final List<Fragment> mFragments = new ArrayList<>();
        private final List<String> mFragmentTitles = new ArrayList<>();
        private FragmentManager fm;


        public Adapter(FragmentManager fm) {
            super(fm);
            this.fm = fm;
            this.saveState();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragments.add(fragment);


            mFragmentTitles.add(title);
        }
        public void clear(){
            mFragmentTitles.clear();;
            mFragments.clear();
            notifyDataSetChanged();
        }


        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitles.get(position);
        }

        @Override
        public int getItemPosition(Object object){
            return PagerAdapter.POSITION_NONE;
        }
    }




}
