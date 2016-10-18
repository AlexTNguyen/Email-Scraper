/*
 ScrapeEmails.java

 A command-program that takes in a domain and prints out all the email addresses found on any of 
 the discoverable pages of the domain.

 */

import java.io.IOException;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.Stack;
import java.util.HashSet;

public class ScrapeEmails {
    // Stack used to hold the remaining pages to parse
    private Stack<String> linksToScrape; 
    private Document doc; 
    private String domain; 
    // HashSet used to ensure an email is only printed once. 
    private HashSet<String> emailSet;
    // HashSet used to ensure a page is only parsed once. 
    private HashSet<String> linkSet; 
    
    public static void main(String[] args) {
        if(args.length == 1) { 
            ScrapeEmails scraper = new ScrapeEmails(args[0]); 
            scraper.findEmails();
        }
        else System.out.println("Please enter a domain!");
        System.out.println("Scraping Complete");
    }

    public ScrapeEmails (String domain) {
        this.domain = domain;
        linksToScrape = new Stack<String>();
        emailSet = new HashSet<String>();
        linkSet = new HashSet<String>(); 
        linkSet.add(formatURL(domain));
        linksToScrape.push(formatURL(domain)); 
    }

    // Method used to parse each page in the links stack for additional links and email addresses. 
    private void findEmails() { 
        try {
            // While stack of links to parse is not empty ...
            if (!linksToScrape.empty()) { 
                String url = linksToScrape.pop(); 
                doc = Jsoup.connect(url).ignoreHttpErrors(true).get();
                Elements pageLinks = doc.select("a[href]");
                // For all the links found on the current page...
                for(Element link: pageLinks) {
                    String newUrl = formatURL(link.attr("href"));
                    // If the link is a discoverable page of the domain given and hasn't been 
                    // parsed before ...
                    if(correctLink(newUrl) && !linkSet.contains(newUrl)) {
                        Connection.Response res = Jsoup.connect(newUrl)
                                                       .ignoreHttpErrors(true)
                                                       .ignoreContentType(true)
                                                       .timeout(0)
                                                       .execute();
                        // Adds only HTML pages and not files like pdf, zip, etc.
                        if(res.contentType().contains("text/html")) {
                            linkSet.add(newUrl);
                            linksToScrape.push(newUrl); 
                        }
                    }     
                }
                // RegEx used to find email addresses.
                String regex = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" + 
                               "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
                Elements emails = doc.select(":matches(" + regex + ")");
                // For all the email address founded on the current page...
                for(Element email : emails) {
                    // If this email has not been printed before then print it. 
                    if(!emailSet.contains(email.text())) {
                        if(emailSet.isEmpty()) System.out.println("Found these email addresses: ");
                        emailSet.add(email.text());
                        System.out.println(email.text());
                    }
                }
                findEmails();
            } 
        } catch (IOException e) {
            e.printStackTrace();
        }  
    }

    // Helper function used to determine if a given link is a discoverable page of the domain 
    // given and is not a subdomain or belonging to an unrelated domain.
    private boolean correctLink(String newUrl) {
        return newUrl.startsWith("http://www." + domain) || 
               newUrl.startsWith("https://www." + domain) || 
               newUrl.startsWith("http://" + domain) || 
               newUrl.startsWith("https://" + domain);
    }

    // Formats the URL so that it could be used by the Jsoup library (must start with http)
    private String formatURL(String url) { 
        while(url.startsWith("/")) {
            url = url.substring(1); 
        }
        if(!(url.contains("http://www.") || url.contains("https://www.") || 
             url.contains("http://") || url.contains("https://"))) {
            url = "http://" + url;
        }
        return url;
    }
}
