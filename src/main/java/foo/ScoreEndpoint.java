package foo;

import java.util.Date;
import java.util.List;
import java.util.Random;

import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.UnauthorizedException;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.QueryResultList;

@Api(name = "myApi",
     version = "v1",
     audiences = "927375242383-t21v9ml38tkh2pr30m4hqiflkl3jfohl.apps.googleusercontent.com",
     clientIds = {"927375242383-t21v9ml38tkh2pr30m4hqiflkl3jfohl.apps.googleusercontent.com",
        "927375242383-jm45ei76rdsfv7tmjv58tcsjjpvgkdje.apps.googleusercontent.com"},
     namespace =
     @ApiNamespace(
           ownerDomain = "helloworld.example.com",
           ownerName = "helloworld.example.com",
           packagePath = "")
     )

public class ScoreEndpoint {

    Random r = new Random();

    // Remember: return primitives and enums are not allowed.
    @ApiMethod(name = "getRandom", httpMethod = HttpMethod.GET)
    public RandomResult random() {
        return new RandomResult(r.nextInt(6) + 1);
    }

    @ApiMethod(name = "hello", httpMethod = HttpMethod.GET)
    public User Hello(User user) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Invalid credentials");
        }
        System.out.println("Yeah:" + user.toString());
        return user;
    }

    // Retrieve the top 10 scores globally
    @ApiMethod(name = "topscores", httpMethod = HttpMethod.GET)
    public CollectionResponse<Entity> topscores() {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query q = new Query("Score").addSort("score", SortDirection.DESCENDING);

        try {
            PreparedQuery pq = datastore.prepare(q);
            List<Entity> result = pq.asList(FetchOptions.Builder.withLimit(10));
            return CollectionResponse.<Entity>builder().setItems(result).build();
        } catch (Exception e) {
            System.err.println("Error fetching top scores: " + e.getMessage());
            return CollectionResponse.<Entity>builder().setItems(null).build();
        }
    }

    // Retrieve the top 10 scores of the specified user
    @ApiMethod(name = "myscores", httpMethod = HttpMethod.GET)
    public CollectionResponse<Entity> myscores(@Named("name") String name) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query q = new Query("Score")
                .setFilter(new FilterPredicate("name", FilterOperator.EQUAL, name))
                .addSort("score", SortDirection.DESCENDING);

        try {
            PreparedQuery pq = datastore.prepare(q);
            List<Entity> result = pq.asList(FetchOptions.Builder.withLimit(10));
            return CollectionResponse.<Entity>builder().setItems(result).build();
        } catch (Exception e) {
            System.err.println("Error fetching user scores: " + e.getMessage());
            return CollectionResponse.<Entity>builder().setItems(null).build();
        }
    }

    // Add a score for the specified user
    @ApiMethod(name = "addScoreSec", httpMethod = HttpMethod.GET)
    public Entity addScoreSec(@Named("name") String name, @Named("score") int score) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Entity e = new Entity("Score", name + score);
        e.setProperty("name", name);
        e.setProperty("score", score);

        try {
            datastore.put(e);
            System.out.println("Score added successfully for user: " + name);
            return e;
        } catch (Exception ex) {
            System.err.println("Error adding score for user " + name + ": " + ex.getMessage());
            return null;
        }
    }

    // Post a message (existing functionality)
    @ApiMethod(name = "postMessage", httpMethod = HttpMethod.POST)
    public Entity postMessage(PostMessage pm) {
        Entity e = new Entity("Post");
        e.setProperty("owner", pm.owner);
        e.setProperty("url", pm.url);
        e.setProperty("body", pm.body);
        e.setProperty("likec", 0);
        e.setProperty("date", new Date());

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        try {
            datastore.put(e);
            System.out.println("Message posted successfully.");
            return e;
        } catch (Exception ex) {
            System.err.println("Error posting message: " + ex.getMessage());
            return null;
        }
    }

    // Retrieve the user's posts (existing functionality)
    @ApiMethod(name = "mypost", httpMethod = HttpMethod.GET)
    public CollectionResponse<Entity> mypost(@Named("name") String name, @Nullable @Named("next") String cursorString) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query q = new Query("Post").setFilter(new FilterPredicate("owner", FilterOperator.EQUAL, name));

        FetchOptions fetchOptions = FetchOptions.Builder.withLimit(2);
        if (cursorString != null) {
            fetchOptions.startCursor(Cursor.fromWebSafeString(cursorString));
        }

        try {
            PreparedQuery pq = datastore.prepare(q);
            QueryResultList<Entity> results = pq.asQueryResultList(fetchOptions);
            cursorString = results.getCursor().toWebSafeString();
            return CollectionResponse.<Entity>builder().setItems(results).setNextPageToken(cursorString).build();
        } catch (Exception ex) {
            System.err.println("Error fetching user posts: " + ex.getMessage());
            return CollectionResponse.<Entity>builder().setItems(null).build();
        }
    }
}

