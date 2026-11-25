package com.consumerreview.helper;
import java.net.URI;
import java.util.List;
public final class DiscoveryTest {
    public static void main(String[] args) throws Exception {
        String html="<a href='https://www.facebook.com/ExampleBrand'>Facebook</a>"
            +"<a href='https://www.facebook.com/ExampleBrand/reviews/'>Reviews</a>"
            +"<a href='https://www.instagram.com/example.brand/'>Instagram</a>"
            +"<a href='/pages/reviews'>Reviews</a><a href='/pages/reviews'>Duplicate</a>"
            +"<a href='https://facebook.com/sharer/sharer.php'>Share</a>"
            +"<a href='https://facebook.com.evil.example/fake'>Fake</a>"
            +"<a href='javascript:alert(1)'>Unsafe</a>"
            +"<script type='application/ld+json'>{\"sameAs\":[\"https://www.youtube.com/@ExampleBrand\"]}</script>";
        List<Discovery.Link> links=Discovery.extract(html,new URI("https://example.com/"));
        if(links.size()!=5)throw new AssertionError("Expected 5 unique candidates; got "+links.size());
        if(links.stream().noneMatch(l->l.category.equals("Facebook review link")))throw new AssertionError("Missing Facebook review link");
        if(links.stream().noneMatch(l->l.category.equals("YouTube channel candidate")))throw new AssertionError("Missing sameAs link");
        if(links.stream().noneMatch(l->l.url.equals("https://example.com/pages/reviews")))throw new AssertionError("Missing relative review link");
        System.out.println("Discovery tests passed: social profiles, Facebook reviews, merchant review links, duplicate and unsafe-link rejection.");
    }
}
