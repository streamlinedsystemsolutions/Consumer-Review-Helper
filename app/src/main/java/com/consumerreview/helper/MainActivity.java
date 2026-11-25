package com.consumerreview.helper;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.graphics.Insets;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;

public final class MainActivity extends Activity {
    private SharedPreferences prefs;
    private LinearLayout root;
    private EditText review, company;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("review_helper", MODE_PRIVATE);
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(48), dp(20), dp(36));
        root.setBackgroundColor(Color.rgb(247,249,252));
        scroll.setFillViewport(true); scroll.addView(root); setContentView(scroll);
        root.setOnApplyWindowInsetsListener((v,insets)-> {
            if (Build.VERSION.SDK_INT >= 30) {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                v.setPadding(dp(20)+bars.left, Math.max(dp(24),bars.top+dp(12)), dp(20)+bars.right, Math.max(dp(24),bars.bottom+dp(12)));
            } else {
                v.setPadding(dp(20),Math.max(dp(24),insets.getSystemWindowInsetTop()+dp(12)),dp(20),Math.max(dp(24),insets.getSystemWindowInsetBottom()+dp(12)));
            }
            return insets;
        });
        root.requestApplyInsets();
        label("Consumer Review Helper", 26);
        label("Your experience. Your account. Your approval.", 15);
        label("Company or product", 18);
        company = editor("company", "", 1);
        label("Your review",18);
        review = editor("review", "", 7);
        button("Copy review", v -> copyReview());
        button("Share review…", v -> {
            if (!hasReview()) return;
            Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,review.getText().toString());
            startActivity(Intent.createChooser(send,"Share your review"));
        });
        label("Find a review page",20);
        label("Open a search for your company, then verify the listing and submit only your own genuine experience.",15);
        button("Search Google for reviews",v->search("customer reviews write a review"));
        button("Search Trustpilot",v->search("site:trustpilot.com/review"));
        button("Search independent product reviews",v->search("product review customer experience"));
        label("Submission reminder",20);
        label("Check the website’s rules, use your own account, and submit only one authentic review per platform. If you already reviewed a site, update your existing review instead.",14);
        label("Consumer complaint portals",20);
        destination("Better Business Bureau","https://www.bbb.org/file-a-complaint","bbb");
        destination("Colorado Attorney General","https://coag.gov/file-complaint/product-and-services/","coag");
        destination("FTC Report Fraud (if applicable)","https://reportfraud.ftc.gov/","ftc");
        label("Keep receipts and correspondence. A checkmark means you personally confirmed submission, not that this app posted anything. The app does not collect passwords or submit reviews automatically.",14);
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private void label(String text,int size){ TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(Color.rgb(25,40,56));t.setPadding(0,dp(12),0,dp(6));root.addView(t); }
    private EditText editor(String key,String fallback,int lines){ EditText e=new EditText(this);e.setText(prefs.getString(key,fallback)); e.setTextColor(Color.rgb(25,40,56));e.setGravity(Gravity.TOP);e.setMinLines(lines);e.setMaxLines(lines==1?2:12); e.setInputType(android.text.InputType.TYPE_CLASS_TEXT | (lines>1?android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE:0));root.addView(e,new LinearLayout.LayoutParams(-1,-2));e.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int b,int c){prefs.edit().putString(key,s.toString()).apply();} public void afterTextChanged(Editable s){} });return e; }
    private void button(String title,View.OnClickListener listener){ Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setMinHeight(dp(48));b.setOnClickListener(listener);root.addView(b,new LinearLayout.LayoutParams(-1,-2)); }
    private boolean hasReview(){ if(review.getText().toString().trim().isEmpty()){review.setError("Enter your review first");return false;}return true; }
    private boolean copyReview(){ if(!hasReview()) return false; ClipboardManager clipboard=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE); if(clipboard==null){Toast.makeText(this,"Clipboard unavailable",Toast.LENGTH_LONG).show();return false;} clipboard.setPrimaryClip(ClipData.newPlainText("Consumer review",review.getText().toString()));Toast.makeText(this,"Review copied",Toast.LENGTH_SHORT).show();return true; }
    private void open(String url){ try { startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url))); }catch(ActivityNotFoundException e){Toast.makeText(this,"No browser installed",Toast.LENGTH_LONG).show();} }
    private void search(String terms){ String name=company.getText().toString().trim();if(name.isEmpty()){company.setError("Enter a company or product");return;}open("https://www.google.com/search?q="+Uri.encode(name+" "+terms)); }
    private void destination(String title,String url,String key){
        button(title+" ↗",v->open(url));
        CheckBox c=new CheckBox(this);
        c.setText("I have personally submitted: "+title);
        c.setTextColor(Color.rgb(25,40,56));
        java.util.function.Supplier<String> caseKey=()->"submitted:"+company.getText().toString().trim().toLowerCase(java.util.Locale.ROOT)+":"+key;
        c.setChecked(prefs.getBoolean(caseKey.get(),false));
        c.setOnClickListener(v->prefs.edit().putBoolean(caseKey.get(),c.isChecked()).apply());
        company.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){} public void onTextChanged(CharSequence s,int st,int before,int count){c.setChecked(prefs.getBoolean(caseKey.get(),false));} public void afterTextChanged(Editable s){} });
        root.addView(c);
    }
}
