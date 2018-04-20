package com.rbmhtechnology.vind.solr.backend.server;

import com.rbmhtechnology.vind.annotations.language.Language;
import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.result.BeanSearchResult;
import com.rbmhtechnology.vind.api.result.SuggestionResult;
import com.rbmhtechnology.vind.configure.SearchConfiguration;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.DocumentFactoryBuilder;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.model.FieldDescriptorBuilder;
import com.rbmhtechnology.vind.solr.backend.SolrSearchServer;
import com.rbmhtechnology.vind.solr.backend.utils.Asset;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Collection;

import static com.rbmhtechnology.vind.api.query.filter.Filter.eq;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 16.06.16.
 */
public class SolrSearchServerTest {

    @Mock
    private SolrClient solrClient;

    @Mock
    private SolrPingResponse solrPingResponse;


    @Mock
    private QueryResponse response;

    @Mock
    private NamedList responseObject;

    @Mock
    private UpdateResponse iResponse;

    private SearchServer server;

    /**
     * Class extending SolrSearchServer to ve able to use
     *
     */
    private class SolrSearchServerTestClass extends SolrSearchServer{
        public SolrSearchServerTestClass(SolrClient solrClient){
            super(solrClient, false);
        }
    }


    @Before
    public void init() throws IOException, SolrServerException {
        MockitoAnnotations.initMocks(this);
        when(solrClient.ping()).thenReturn(solrPingResponse);
        when(solrPingResponse.getStatus()).thenReturn(0);
        when(solrPingResponse.getQTime()).thenReturn(10);

        when(solrClient.query(any(), any(SolrRequest.METHOD.class))).thenReturn(response);
        when(response.getResults()).thenReturn(new SolrDocumentList());
        when(response.getResponse()).thenReturn(responseObject);
        when(responseObject.get("responseHeader")).thenReturn(responseObject);
        when(responseObject.get("params")).thenReturn(responseObject);
        when(responseObject.get("suggestion.field")).thenReturn("category");

        when(solrClient.add(org.mockito.Matchers.<Collection<SolrInputDocument>>any())).thenReturn(iResponse);
        when(solrClient.add(any(SolrInputDocument.class))).thenReturn(iResponse);
        when(iResponse.getQTime()).thenReturn(10);
        when(iResponse.getElapsedTime()).thenReturn(15l);

        //we use the protected constructor to avoid schema checking
        server = new SolrSearchServerTestClass(solrClient);
    }

    @Test
    public void testExecute() throws Exception {
        Asset asset = new Asset();

        //FIXME: Asset has no @Id field
        server.indexBean(asset);

        //query
        BeanSearchResult<Asset> result = server.execute(Search.fulltext("hello world").filter(eq("category", "test")), Asset.class);

        //suggestion
        SuggestionResult suggestions = server.execute(Search.suggest("he").fields("category"), Asset.class);

        FieldDescriptor<String> title = new FieldDescriptorBuilder()
                .setBoost(2)
                .setLanguage(Language.German).buildTextField("title");

        //complex
        DocumentFactory factory = new DocumentFactoryBuilder("asset")
                .addField(title)
                .build();

        Document document = factory.createDoc("1234");

        server.index(document);

        //suggestion
        SuggestionResult suggestionsFromFactory = server.execute(Search.suggest("he").fields("title"), factory);

    }

    //is ignored because tests (and therefor requires) a remote solr server
    @Test
    @Ignore
    public void testExecuteReal() throws Exception {

        SearchConfiguration.set(SearchConfiguration.SERVER_HOST, "http://localhost:8983/solr/searchindex");

        SearchServer server = SearchServer.getInstance();

        FieldDescriptor<String> title = new FieldDescriptorBuilder()
                .setBoost(2)
                .setLanguage(Language.German).buildTextField("title");

        DocumentFactory factory = new DocumentFactoryBuilder("asset")
                .addField(title)
                .build();

        server.index(factory.createDoc("1").setValue(title, "Hello World"));
        server.commit();

        assertEquals(1, server.execute(Search.fulltext(), factory).getNumOfResults());
    }

    //is ignored because tests (and therefor requires) a remote solr server
    @Test
    @Ignore
    public void testConfigureCoreReal() throws Exception {

        SearchConfiguration.set(SearchConfiguration.SERVER_HOST, "http://localhost:8983/solr");
        SearchServer server = SearchServer.getInstance("assets");
        server.commit();
    }

    //is ignored because tests (and therefor requires) a remote solr server
    @Test
    @Ignore
    public void testConfigureCoreHostReal() throws Exception {

        SearchServer server = SearchServer.getInstance("http://localhost:8983/solr","assets");
        server.commit();
    }

    //is ignored because tests (and therefor requires) a remote solr cloud server
    @Test
    @Ignore
    public void testConfigureCloudCoreHostReal() throws Exception {
        SearchConfiguration.set(SearchConfiguration.SERVER_SOLR_CLOUD, true);
        SearchServer server = SearchServer.getInstance("localhost:9983","searchindex");
        server.commit();
    }

    @Test
    @Ignore
    public void testConfigureProviderHostCoreReal() throws Exception {

        SearchServer server = SearchServer.getInstance("RemoteSolrServerProvider","http://localhost:8983/solr","assets");
        server.commit();
    }
}