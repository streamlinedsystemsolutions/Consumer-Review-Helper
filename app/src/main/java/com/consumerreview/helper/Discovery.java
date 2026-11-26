package com.consumerreview.helper;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

final class Discovery {
    static final class Link {
        final String category,url;
        Link(String category,String url){this.category=category;this.url=url;}
    }
    private static final Pattern HREF=Pattern.compile("(?is)<a\\b[^>]*?\\bhref\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))");
    private static final Pattern SAME_AS=Pattern.compile("(?is)\"sameAs\"\\s*:\\s*\\[([^\\]]{0,16000})\\]");
    private static final Pattern JSON_URL=Pattern.compile("\"(https?://[^\"\\s]+)\"");
    private static boolean hostIs(String h,String domain){return h.equals(domain)||h.endsWith("."+domain);}
    private static String host(URI u){return u.getHost()==null?"":u.getHost().toLowerCase(Locale.ROOT);}
    private static String baseHost(String h){return h.startsWith("www.")?h.substring(4):h;}
    private static String classify(URI u, URI base){
        String h=host(u),p=u.getPath()==null?"":u.getPath().toLowerCase(Locale.ROOT);
        if(p.isEmpty()||p.equals("/"))return null;
        if(hostIs(h,"facebook.com")&&!p.matches("^/(sharer[^/]*|share[^/]*|plugins|dialog|login|watch|reel|groups)(/.*)?$"))return p.contains("/reviews")?"Facebook review link":"Facebook profile candidate";
        if(hostIs(h,"instagram.com")&&!p.matches("^/(p|reel|stories|explore|accounts)(/.*)?$"))return "Instagram profile candidate";
        if(hostIs(h,"youtube.com")&&(p.startsWith("/@")||p.startsWith("/channel/")||p.startsWith("/c/")||p.startsWith("/user/")))return "YouTube channel candidate";
        if((hostIs(h,"x.com")||hostIs(h,"twitter.com"))&&!p.matches("^/(intent|share|search|home|i)(/.*)?$"))return "X profile candidate";
        if(hostIs(h,"tiktok.com")&&p.startsWith("/@"))return "TikTok profile candidate";
        if(hostIs(h,"linkedin.com")&&p.startsWith("/company/"))return "LinkedIn company profile candidate";
        if((hostIs(h,"threads.com")||hostIs(h,"threads.net"))&&p.startsWith("/@"))return "Threads profile candidate";
        if(hostIs(h,"pinterest.com")&&!p.startsWith("/pin/")&&!p.startsWith("/search/"))return "Pinterest profile candidate";
        for(String review:new String[]{"trustpilot.com","yelp.com","bbb.org","sitejabber.com","smartcustomer.com","reviews.io","resellerratings.com","scamadviser.com","consumeraffairs.com","judge.me","loox.io"})if(hostIs(h,review))return "External feedback link";
        if(baseHost(h).equals(baseHost(host(base)))&&(p.contains("review")||p.contains("testimonial")||p.contains("feedback")||p.contains("reward")||p.contains("contact")||p.contains("support")))return "Merchant feedback or support link";
        return null;
    }
    private static void add(Map<String,Link> results, URI base,String raw){
        if(results.size()>=40||raw==null||raw.length()>2000)return;
        try{
            URI u=base.resolve(raw.trim().replace("&amp;","&").replace("\\/","/"));
            if(!"https".equalsIgnoreCase(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null)return;
            String category=classify(u,base);if(category==null)return;
            // Preserve the identifier on Facebook numeric profile links; drop tracking parameters.
            String query=null;
            if(hostIs(host(u),"facebook.com") && "/profile.php".equalsIgnoreCase(u.getPath())){
                Matcher id=Pattern.compile("(?:^|&)id=([0-9]{1,30})(?:&|$)").matcher(u.getRawQuery()==null?"":u.getRawQuery());
                if(!id.find())return;
                query="id="+id.group(1);
            }
            URI cleaned=new URI("https",null,u.getHost(),u.getPort(),u.getPath(),query,null);
            String url=cleaned.toASCIIString();results.putIfAbsent(url,new Link(category,url));
        }catch(Exception ignored){}
    }
    static List<Link> extract(String html,URI base){
        Map<String,Link> out=new LinkedHashMap<>();
        Matcher m=HREF.matcher(html);
        while(m.find()&&out.size()<40)add(out,base,m.group(1)!=null?m.group(1):m.group(2)!=null?m.group(2):m.group(3));
        Matcher a=SAME_AS.matcher(html);
        while(a.find()&&out.size()<40){Matcher j=JSON_URL.matcher(a.group(1));while(j.find()&&out.size()<40)add(out,base,j.group(1));}
        return new ArrayList<>(out.values());
    }
    private static URI validate(String input)throws Exception{
        URI u=new URI(input);
        String h=host(u);
        if(!"https".equalsIgnoreCase(u.getScheme())||h.isEmpty()||u.getUserInfo()!=null||!h.contains(".")||(u.getPort()!=-1&&u.getPort()!=443))throw new IOException("Enter a public HTTPS website");
        for(InetAddress ip:InetAddress.getAllByName(h))if(ip.isAnyLocalAddress()||ip.isLoopbackAddress()||ip.isLinkLocalAddress()||ip.isSiteLocalAddress()||ip.isMulticastAddress())throw new IOException("Public sites only");
        return new URI("https",null,h,-1,"/",null,null);
    }
    static List<Link> scan(String input)throws Exception{
        URI base=validate(input);
        for(int redirects=0;redirects<3;redirects++){
            HttpURLConnection c=(HttpURLConnection)base.toURL().openConnection();
            try{
                c.setConnectTimeout(7000);c.setReadTimeout(7000);c.setInstanceFollowRedirects(false);
                c.setRequestProperty("Accept","text/html");
                int status=c.getResponseCode();
                if(status>=300&&status<400){
                    String location=c.getHeaderField("Location");if(location==null)throw new IOException("Missing redirect");
                    URI next=base.resolve(location);URI valid=validate(next.toString());
                    if(!baseHost(host(valid)).equals(baseHost(host(base))))throw new IOException("Different website redirect");
                    base=new URI("https",null,valid.getHost(),-1,next.getPath(),next.getQuery(),null);continue;
                }
                if(status!=200)throw new IOException("Website returned "+status);
                try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                    byte[] b=new byte[8192];int n,remaining=1500000;
                    while(remaining>0&&(n=in.read(b,0,Math.min(b.length,remaining)))!=-1){out.write(b,0,n);remaining-=n;}
                    return extract(out.toString(StandardCharsets.UTF_8.name()),base);
                }
            }finally{c.disconnect();}
        }
        throw new IOException("Too many redirects");
    }
}
