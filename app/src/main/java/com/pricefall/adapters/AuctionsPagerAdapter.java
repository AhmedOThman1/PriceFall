package com.pricefall.adapters;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.pricefall.pojo.Auction;
import com.pricefall.ui.fragments.seller.auctions.tab.ShowAuctionsTabFragment;

public class AuctionsPagerAdapter extends FragmentPagerAdapter {
    Context context;

    public AuctionsPagerAdapter(@NonNull FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        Fragment fragment = null;
        switch (position) {
            case 0:
                fragment = new ShowAuctionsTabFragment(Auction.UPCOMING);
                break;
            case 1:
                fragment = new ShowAuctionsTabFragment(Auction.LIVE);
                break;
            case 2:
                fragment = new ShowAuctionsTabFragment(Auction.DONE);
                break;

        }
        return fragment;
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return (
                position == 0 ? "Upcoming" :
                        position == 1 ?  "Live" : "Done" );
    }

}
