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
    private EditText review, company, aliases, domain, product;
    private Spinner engine, identity;
    private LinearLayout discovered, savedPages;
    private boolean scanning;
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
        label("Company discovery",20);
        label("Add alternate business names, the official website and product name for more precise results.",15);
        label("Other business names (separate with commas)",15);
        aliases=editor("aliases","",2);
        label("Official website (optional)",15);
        domain=editor("domain","",1);
        label("Product or model (optional)",15);
        product=editor("product","",1);
        label("Search engine",15);
        engine=new Spinner(this);
        engine.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Google","Bing","DuckDuckGo"}));
        root.addView(engine);
        label("Search identity",15);
        identity=new Spinner(this);
        identity.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"All entered names","Main company","Alternate names","Website domain","Product or model"}));
        root.addView(identity);
        button("Discover links on official website",v->discover());
        label("Links found on the official website (review eligibility not verified)",15);
        discovered=new LinearLayout(this);discovered.setOrientation(LinearLayout.VERTICAL);root.addView(discovered);
        label("Saved review and support pages",20);
        label("Save pages you have checked yourself. Links and status stay on your device and are grouped by company.",15);
        button("Add a checked page URL",v->addPage());
        savedPages=new LinearLayout(this);savedPages.setOrientation(LinearLayout.VERTICAL);root.addView(savedPages);
        refreshSaved();
        company.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int b,int c){discovered.removeAllViews();refreshSaved();} public void afterTextChanged(Editable s){} });
        label("Independent review platforms",20);
        query("General customer reviews","customer reviews write a review");
        query("Trustpilot","site:trustpilot.com/review");
        query("Yelp","site:yelp.com");
        query("Google Maps / business profile","Google Maps business reviews");
        query("BBB business profiles","site:bbb.org business profile");
        query("Sitejabber / SmartCustomer","(site:sitejabber.com OR site:smartcustomer.com)");
        query("ResellerRatings","site:resellerratings.com");
        query("Reviews.io","site:reviews.io");
        query("ScamAdviser","site:scamadviser.com");
        query("ConsumerAffairs","site:consumeraffairs.com");
        query("Industry-specific review sites","customer product reviews independent review website");
        label("Social accounts and recommendations",20);
        query("Facebook reviews / recommendations","site:facebook.com reviews recommendations");
        query("Facebook company page","site:facebook.com");
        query("Instagram official account","site:instagram.com");
        query("YouTube official channel","site:youtube.com official");
        query("X / Twitter customer support","(site:x.com OR site:twitter.com)");
        query("TikTok company account","site:tiktok.com");
        query("LinkedIn company page","site:linkedin.com/company");
        query("Threads company account","site:threads.com");
        query("Reddit product discussion","site:reddit.com product review");
        label("Manufacturer and product feedback",20);
        query("Manufacturer product reviews","product reviews write a review");
        query("Merchant review invitation / rewards","customer review rewards review invitation");
        query("Retailer review pages (only if eligible)","product retailer customer reviews");
        query("Customer service / warranty","official support warranty contact");
        label("These search buttons open the selected search engine. They do not claim a matching listing exists. Social accounts may offer only support or discussion, not reviews. Use retailer reviews only when you meet their eligibility requirements.",14);
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
    private void query(String title,String terms){button(title+" ↗",v->search(terms));}
    private String quoted(String s){return "\""+s.trim().replace("\"","")+"\"";}
    private String identityQuery(){
        java.util.LinkedHashSet<String> names=new java.util.LinkedHashSet<>();
        String main=company.getText().toString().trim();
        String other=aliases.getText().toString();
        String web=domain.getText().toString().trim().replaceFirst("(?i)^https?://","").split("/",2)[0];
        String model=product.getText().toString().trim();
        int mode=identity.getSelectedItemPosition();
        if((mode==0||mode==1)&&!main.isEmpty())names.add(quoted(main));
        if(mode==0||mode==2)for(String a:other.split("[,;\\n]"))if(!a.trim().isEmpty())names.add(quoted(a));
        if((mode==0||mode==3)&&!web.isEmpty())names.add(quoted(web));
        if(mode==4&&!model.isEmpty())names.add(quoted(model));
        if(names.isEmpty())return "";
        String base=names.size()==1?names.iterator().next():"("+String.join(" OR ",names)+")";
        return base;
    }
    private void search(String terms){
        String q=identityQuery();
        if(q.isEmpty()){Toast.makeText(this,"Enter an identity for the selected search mode",Toast.LENGTH_LONG).show();return;}
        String[] urls={"https://www.google.com/search?q=","https://www.bing.com/search?q=","https://duckduckgo.com/?q="};
        open(urls[engine.getSelectedItemPosition()]+Uri.encode(q+" "+terms));
    }
    private void showDiscovery(String message){TextView t=new TextView(this);t.setText(message);t.setTextColor(Color.rgb(25,40,56));t.setPadding(0,dp(6),0,dp(6));discovered.addView(t);}
    private void discover(){
        if(scanning)return;
        String value=domain.getText().toString().trim();
        if(value.isEmpty()){domain.setError("Enter the official website");return;}
        if(!value.contains("://"))value="https://"+value;
        final String site=value;
        scanning=true;discovered.removeAllViews();showDiscovery("Checking public homepage links…");
        new Thread(()->{
            try{
                java.util.List<Discovery.Link> links=Discovery.scan(site);
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    scanning=false;discovered.removeAllViews();
                    if(links.isEmpty())showDiscovery("No matching public links found. Try the targeted searches below.");
                    else for(Discovery.Link link:links){showDiscovery(link.category+" — "+link.url);Button b=new Button(this);b.setText("Open link");b.setAllCaps(false);b.setOnClickListener(v->open(link.url));discovered.addView(b);}
                });
            }catch(Exception e){runOnUiThread(()->{if(isFinishing()||isDestroyed())return;scanning=false;discovered.removeAllViews();showDiscovery("Could not scan the public homepage. It may block access or require JavaScript. Use the targeted searches below.");});}
        }).start();
    }
    private String savedKey(){return "pages:"+company.getText().toString().trim().toLowerCase(java.util.Locale.ROOT);}
    private String pageUrl(String raw){
        try{
            java.net.URI u=new java.net.URI(raw.trim());
            if(!"https".equalsIgnoreCase(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||raw.length()>2000)return null;
            return u.toASCIIString();
        }catch(Exception e){return null;}
    }
    private void addPage(){
        if(company.getText().toString().trim().isEmpty()){company.setError("Enter the company first");return;}
        EditText input=new EditText(this);input.setSingleLine(true);input.setHint("https://…");input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("Save a page you checked").setMessage("Use a genuine company review or customer support page.").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        dialog.setOnShowListener(d->dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String url=pageUrl(input.getText().toString());if(url==null){input.setError("Enter a valid HTTPS URL");return;}
            savePage(url);dialog.dismiss();
        }));dialog.show();
    }
    private void savePage(String url){
        if(company.getText().toString().trim().isEmpty())return;
        String key=savedKey();
        java.util.Set<String> pages=new java.util.HashSet<>(prefs.getStringSet(key,java.util.Collections.emptySet()));
        if(pages.size()>=60&&!pages.contains(url)){Toast.makeText(this,"Saved page limit reached",Toast.LENGTH_SHORT).show();return;}
        pages.add(url);prefs.edit().putStringSet(key,pages).apply();refreshSaved();
    }
    private void refreshSaved(){
        if(savedPages==null)return;
        savedPages.removeAllViews();
        if(company.getText().toString().trim().isEmpty())return;
        final String key=savedKey();
        java.util.List<String> pages=new java.util.ArrayList<>(prefs.getStringSet(key,java.util.Collections.emptySet()));java.util.Collections.sort(pages);
        for(String url:pages){
            TextView t=new TextView(this);t.setText(url);t.setTextColor(Color.rgb(25,40,56));savedPages.addView(t);
            Button openButton=new Button(this);openButton.setText("Open saved page");openButton.setAllCaps(false);openButton.setOnClickListener(v->open(url));savedPages.addView(openButton);
            CheckBox checked=new CheckBox(this);checked.setText("Already submitted / resolved");checked.setChecked(prefs.getBoolean(key+":done:"+url,false));checked.setOnCheckedChangeListener((b,c)->prefs.edit().putBoolean(key+":done:"+url,c).apply());savedPages.addView(checked);
            Button remove=new Button(this);remove.setText("Remove saved page");remove.setAllCaps(false);remove.setOnClickListener(v->{java.util.Set<String> updated=new java.util.HashSet<>(prefs.getStringSet(key,java.util.Collections.emptySet()));updated.remove(url);prefs.edit().putStringSet(key,updated).remove(key+":done:"+url).apply();refreshSaved();});savedPages.addView(remove);
        }
    }
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
