
package com.pricefall.ui.fragments.seller.sales;

import static com.pricefall.ui.activities.MainActivity.setTextWithAnimation;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.Utils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pricefall.R;
import com.pricefall.databinding.FragmentSellerStatisticsBinding;
import com.pricefall.pojo.Auction;
import com.pricefall.pojo.Payment;
import com.pricefall.pojo.User;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SellerStatisticsFragment extends Fragment {

    FragmentSellerStatisticsBinding binding;
    FirebaseDatabase database;
    DatabaseReference usersRef, auctionsRef, paymentsRef;
    FirebaseUser firebaseUser;

    ArrayList<Payment> tempPaymentItems = new ArrayList<>();


    public SellerStatisticsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentSellerStatisticsBinding.inflate(inflater, container, false);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("Users");
        auctionsRef = database.getReference("Auctions");
        paymentsRef = database.getReference("Payments");

        binding.totalIncomeChart.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    binding.scroll.requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    binding.scroll.requestDisallowInterceptTouchEvent(false);
                    break;
            }
            return binding.totalIncomeChart.onTouchEvent(event);
        });
        binding.auctionsChartBarChart.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    binding.scroll.requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    binding.scroll.requestDisallowInterceptTouchEvent(false);
                    break;
            }
            return binding.totalIncomeChart.onTouchEvent(event);
        });

        binding.totalIncomeChart.setNoDataText("No chart data available for the total earned money");
        binding.totalIncomeBarChart.setNoDataText("No chart data available for the total earned money");

        binding.auctionsChartBarChart.setNoDataText("No chart data available for your auctions");

        getUsers();

        return binding.getRoot();
    }


    Map<String, User> usersMap = new HashMap<>();

    private void getUsers() {
        // Read from the database
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                usersMap = new HashMap<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    User item = dataSnapshot.getValue(User.class);
                    assert item != null;
                    usersMap.put(item.id, item);
                }

                getAuction();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    Map<String, Auction> auctionsMap = new HashMap<>();

    private void getAuction() {
        // Read from the database
        auctionsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                auctionsMap = new HashMap<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Auction item = dataSnapshot.getValue(Auction.class);
                    assert item != null;
                    item.seller = usersMap.get(item.sellerId);
                    auctionsMap.put(item.id, item);
                }

                getPayments();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    ArrayList<Entry> entries = new ArrayList<>();
    ArrayList<BarEntry> barEntries = new ArrayList<>();
    ArrayList<BarEntry> auctionBarEntries = new ArrayList<>();
    Map<Long, Double> moneyDateMap = new HashMap<>();
    Map<String, Double> moneyAuctionMap = new HashMap<>();
    ArrayList<Long> dates = new ArrayList<>();
    ArrayList<String> categories = new ArrayList<>();

    double todayIncome = 0, totalIncome = 0, thisMonthIncome = 0;
    ArrayList<Payment> paymentItems = new ArrayList<>();

    private void getPayments() {
        // Read from the database
        paymentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                paymentItems = new ArrayList<>();
                tempPaymentItems = new ArrayList<>();
                barEntries = new ArrayList<>();
                entries = new ArrayList<>();
                auctionBarEntries = new ArrayList<>();
                dates = new ArrayList<>();

                moneyDateMap = new HashMap<>();
                moneyAuctionMap = new HashMap<>();

                todayIncome = 0;
                totalIncome = 0;
                thisMonthIncome = 0;
                Calendar today = Calendar.getInstance();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Payment item = dataSnapshot.getValue(Payment.class);
                    assert item != null;
                    item.auction = auctionsMap.get(item.auctionId);
                    item.user = usersMap.get(item.userId);
                    if (item.userId.equals(firebaseUser.getUid()) ||
                            item.auction.sellerId.equals(firebaseUser.getUid())) {
                        paymentItems.add(item);
                    }
                }


                paymentItems.sort(Comparator.comparing(p -> p.date, Comparator.reverseOrder()));
                tempPaymentItems.addAll(paymentItems);

                ////////////////////////////////////////////////////////////////////////////////////////////////////
                ////////////////////////////////////////////////////////////////////////////////////////////////////
                ////////////////////////////////////////////////////////////////////////////////////////////////////

                for (Payment item : paymentItems) {

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(item.date);
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);

                    totalIncome += item.totalAmount;
                    if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                            calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH)) {
                        thisMonthIncome += item.totalAmount;
                        if (calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH))
                            todayIncome += item.totalAmount;
                    }

                    moneyDateMap.merge(calendar.getTimeInMillis(), item.totalAmount, Double::sum);
                    moneyAuctionMap.merge(item.auction.category, item.totalAmount, Double::sum);
                }


                setTextWithAnimation(binding.todayMoneyNum, todayIncome);
                setTextWithAnimation(binding.thisMonthMoneyNum, thisMonthIncome);
                setTextWithAnimation(binding.totalMoneyNum, totalIncome);

                ArrayList<Pair<Long, Double>> pairs = new ArrayList<>();
                for (Map.Entry<Long, Double> entry : moneyDateMap.entrySet()) {
                    pairs.add(new Pair<>(entry.getKey(), entry.getValue()));
                }

                pairs.sort(Comparator.comparing(p -> p.first));

                for (Pair<Long, Double> pair : pairs) {
                    dates.add(pair.first);
                    entries.add(new Entry(dates.size() - 1, pair.second.floatValue()));
                    barEntries.add(new BarEntry(dates.size() - 1, pair.second.floatValue()));
                }

                for (Map.Entry<String, Double> entry : moneyAuctionMap.entrySet()) {
                    categories.add(entry.getKey());
                    auctionBarEntries.add(new BarEntry(categories.size()-1, entry.getValue().floatValue()));
                }

                if (!entries.isEmpty() && entries.size() > 1) {
                    binding.totalIncomeBarChart.setVisibility(View.GONE);
                    binding.totalIncomeChart.setVisibility(View.VISIBLE);
                    setupLineChart(binding.totalIncomeChart, entries);
                } else if (barEntries.size() == 1) {
                    binding.totalIncomeBarChart.setVisibility(View.VISIBLE);
                    binding.totalIncomeChart.setVisibility(View.GONE);
                    setupBarChart(binding.totalIncomeBarChart, barEntries, false);
                }


                if (!auctionBarEntries.isEmpty() && auctionBarEntries.size() > 1) {
                    setupBarChart(binding.auctionsChartBarChart, auctionBarEntries, true);
                }


                ////////////////////////////////////////////////////////////////////////////////////////////////////
                ////////////////////////////////////////////////////////////////////////////////////////////////////
                ////////////////////////////////////////////////////////////////////////////////////////////////////

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////
    private void setupLineChart(LineChart chart, ArrayList<Entry> chartList) {
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);


        /////////////////
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

//        xAxis.setCenterAxisLabels(true);
//        xAxis.setGranularityEnabled(true);
//        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setLabelCount(chartList.size());
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {

                Log.w("VAL", "" + value);
                int index = ((int) value);
                if (index >= 0 && index < dates.size())
                    return new SimpleDateFormat("d MMM", Locale.getDefault()).format(new Date(dates.get(index)));
                else return "";
            }
        });


        YAxis yAxis = chart.getAxisLeft();
        yAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return new DecimalFormat("$#,###")
                        .format(value);
            }

            @Override
            public String getBarLabel(BarEntry barEntry) {
                return new DecimalFormat("$#,###")
                        .format(barEntry.getY());
            }

            @Override
            public String getFormattedValue(float value) {
                return new DecimalFormat("$#,###")
                        .format(value);
            }
        });

        chart.getAxisRight().setEnabled(false);

        chart.animateXY(2000, 1500);
        /////////

        LineDataSet set1;
        if (chart.getData() != null &&
                chart.getData().getDataSetCount() > 0) {
            set1 = (LineDataSet) chart.getData().getDataSetByIndex(0);
            set1.setValues(chartList);
            set1.notifyDataSetChanged();
            chart.getLineData().notifyDataChanged();
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();
        } else {
            // create a dataset and give it a type
            set1 = new LineDataSet(chartList, "Total payments");

            set1.setDrawIcons(false);
            //////////////////////////////////////////////////////////////////////////////////////
            //////////////////////////////////////////////////////////////////////////////////////
            //////////////////////////////////////////////////////////////////////////////////////
            //////////////////////////////////////////////////////////////////////////////////////
            //////////////////////////////////////////////////////////////////////////////////////
            //////////////////////////////////////////////////////////////////////////////////////

            // draw dashed line
//            set1.enableDashedLine(10f, 5f, 0f);

            // black lines and points
            set1.setColor(requireContext().getResources().getColor(R.color.colorPrimary));
            set1.setCircleColor(requireContext().getResources().getColor(R.color.colorPrimary));

            // line thickness and point size
            set1.setLineWidth(1f);
            set1.setCircleRadius(1f);

            // draw points as solid circles
            set1.setDrawCircleHole(false);

            // customize legend entry
            set1.setFormLineWidth(1f);
//            set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            set1.setFormSize(15.f);

            // text size of values
            set1.setValueTextSize(9f);

            // draw selection line as dashed
//            set1.enableDashedHighlightLine(10f, 5f, 0f);

            // set the filled area
            set1.setDrawFilled(true);
            // set color of filled area
            if (Utils.getSDKInt() >= 18) {
                // drawables only supported on api level 18 and above
                Drawable drawable = ContextCompat.getDrawable(requireContext(), android.R.color.transparent);
                set1.setFillDrawable(drawable);
            } else {
                set1.setFillColor(requireActivity().getResources().getColor(R.color.shimmer));
            }


            set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);

            ArrayList<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1); // add the data sets


            // create a data object with the data sets
            LineData data = new LineData(dataSets);
            // set data
            data.setValueTextSize(10f);
            xAxis.setAxisMinimum(data.getXMin() - .5f);
            xAxis.setAxisMaximum(data.getXMax() + .5f);
            data.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return new DecimalFormat("$#,###.##")
                            .format(value);
                }
            });
            chart.setData(data);
            //////////////////////////////////////////////////////////////////////////////////////
            //////////////////////////////////////////////////////////////////////////////////////
            //////////////////////////////////////////////////////////////////////////////////////
            //////////////////////////////////////////////////////////////////////////////////////
            //////////////////////////////////////////////////////////////////////////////////////
            //////////////////////////////////////////////////////////////////////////////////////

        }
    }

    private void setupBarChart(BarChart chart, ArrayList<BarEntry> chartList,boolean isAuction) {
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);


        /////////////////
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

//        xAxis.setCenterAxisLabels(true);
//        xAxis.setGranularityEnabled(true);
//        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setLabelCount(chartList.size());
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {

                Log.w("VAL", "" + value);
                int index = ((int) value);
                if(isAuction)
                {
                    String title = categories.get(index);
                    String shortTitle = title.length() > 16 ? title.substring(0, 13) + "..." : title;
                    return shortTitle;
                }
                if (index >= 0 && index < dates.size())
                    return new SimpleDateFormat("d MMM", Locale.getDefault()).format(new Date(dates.get(index)));
                else return "";
            }
        });


        YAxis yAxis = chart.getAxisLeft();
        yAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return new DecimalFormat("$#,###")
                        .format(value);
            }

            @Override
            public String getBarLabel(BarEntry barEntry) {
                return new DecimalFormat("$#,###")
                        .format(barEntry.getY());
            }

            @Override
            public String getFormattedValue(float value) {
                return new DecimalFormat("$#,###")
                        .format(value);
            }
        });

        chart.getAxisRight().setEnabled(false);

        chart.animateY(1400);
        /////////

        BarDataSet set1;
        if (chart.getData() != null &&
                chart.getData().getDataSetCount() > 0) {
            set1 = (BarDataSet) chart.getData().getDataSetByIndex(0);
            set1.setValues(chartList);
            set1.notifyDataSetChanged();
            chart.getBarData().notifyDataChanged();
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();
        } else {
            // create a dataset and give it a type
            set1 = new BarDataSet(chartList, isAuction?"Categories sales":"Total sales");

            set1.setDrawIcons(false);

            // draw dashed line
//            set1.enableDashedLine(10f, 5f, 0f);

            // black lines and points
            set1.setColor(requireContext().getResources().getColor(R.color.colorSecondary));

            ArrayList<IBarDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1); // add the data sets

            // create a data object with the data sets
            BarData data = new BarData(dataSets);
            data.setValueTextSize(10f);
            data.setBarWidth(0.5f);
            xAxis.setAxisMinimum(data.getXMin() - .5f);
            xAxis.setAxisMaximum(data.getXMax() + .5f);
            data.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return new DecimalFormat("$#,###.##")
                            .format(value);
                }
            });
            // set data
            chart.setData(data);
        }
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////


}