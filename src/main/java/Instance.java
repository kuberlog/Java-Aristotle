import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import subtitleDownloader.Runner;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class Instance {

    URL solrVideoURL, solrBlockURL;

    List<VideoSource> videoSources;

    List<URL> urlsToIndex;

    Searcher searcher;
    Indexer indexer;
    Renderer renderer;
    Runner stlRunner;

    File stlURLInput;
    File stlDir;

    private String name, username;
    private String backButtonURL, backButtonText, searchBarText;

    Instance() throws MalformedURLException {
        renderer = Renderer.getInstance();
        initializeSQL();
    }

    public String home(){
        return renderer.home(this);
    }

    public String search(String searchText) throws IOException, SolrServerException {
        SearchResult result = searcher.search(searchText);
        return renderer.search(this,result);
    }

    private void initializeSQL(){}

    public void initializeSolr(SolrConfig videoConfig, SolrConfig blockConfig) throws MalformedURLException {

        solrVideoURL = videoConfig.getURL();
        solrBlockURL = blockConfig.getURL();

        SolrClient videoConnection = new HttpSolrClient.Builder(videoConfig.getURL().toString()).build();
        SolrClient blockConnection = new HttpSolrClient.Builder(blockConfig.getURL().toString()).build();


        searcher = new Searcher(videoConnection, blockConnection);
        indexer = new Indexer(videoConnection, blockConnection);
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getUsername(){
        return username;
    }

    public void setUsername(String username){
        this.username = username;
    }

    void downloadSRTs(){
        stlRunner = new Runner(stlURLInput.getAbsolutePath(), stlDir.getAbsolutePath());
    }

    void generateURLsToIndex(){
        urlsToIndex = new ArrayList<URL>();
        for(Iterator<VideoSource> it = videoSources.iterator(); it.hasNext();){
            VideoSource source = it.next();
            OffsetDateTime lastIndexedDate = getLastIndexedVideoFromSource(source.getID());
            List<Video> toAdd = source.getVideosPublishedSince(lastIndexedDate);

            addVideosToURLsToIndex(toAdd);
        }
    }

    private void addVideosToURLsToIndex(List<Video> toAdd) {
        for(Iterator<Video> it = toAdd.iterator(); it.hasNext();){
            urlsToIndex.add(it.next().getUrl());
        }
    }

    public void save(){
    }

    private OffsetDateTime getLastIndexedVideoFromSource(String sourceId) {
        return OffsetDateTime.now();
    }

    URL getSolrVideoURL()  {
        return solrVideoURL;
    }

    URL getSolrBlockURL(){
        return solrBlockURL;
    }

    public String getBackButtonURL() { return backButtonURL; }

    public void setBackButtonURL(String backButtonURL) { this.backButtonURL = backButtonURL;  }

    public String getBackButtonText() { return backButtonText; }

    public void setBackButtonText(String backButtonText) { this.backButtonText = backButtonText; }

    public String getSearchBarText() { return searchBarText; }

    public void setSearchBarText(String searchBarText) { this.searchBarText=searchBarText; }

    private void setBlockConfig(SolrConfig fromID) {

    }

    private void setVideoConfig(SolrConfig fromID) {
    }



    static class InstanceDBExtractor extends SimpleDBResultExtractor<Instance>{

        @Override
        public void extractInstancesFromDBResult(ResultSet rs) {
            try {
                while (rs.next()) {
                    Instance config = new Instance();
                    config.setName(rs.getString("name"));
                    config.setUsername(rs.getString("username"));
                    config.setBackButtonText(rs.getString("back-button-text"));
                    config.setBackButtonURL(rs.getString("back-button-url"));
                    config.setSearchBarText(rs.getString("set-search-bar-text"));
                    int videoConfigId = rs.getInt("videoSolrConfigID");
                    int blockConfigId = rs.getInt("blockSolrConfigID");
                    config.setVideoConfig(SolrConfig.fromID(videoConfigId));
                    config.setBlockConfig(SolrConfig.fromID(blockConfigId));

                    instances.add(config);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }
}
