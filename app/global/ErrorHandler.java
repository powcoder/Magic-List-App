https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package global;

import com.typesafe.config.Config;
import controllers.BaseController;
import play.Environment;
import play.api.OptionalSourceMapper;
import play.api.routing.Router;
import play.http.DefaultHttpErrorHandler;
import play.mvc.Http;
import play.mvc.Result;
import views.html.failure.*;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static play.mvc.Results.*;

/**
 *
 */
@Singleton
public class ErrorHandler extends DefaultHttpErrorHandler {

    @Inject
    public ErrorHandler(Config configuration, Environment environment, OptionalSourceMapper sourceMapper,
                        Provider<Router> routes) {
        super(configuration, environment, sourceMapper, routes);
    }

    @Override
    public CompletionStage<Result> onClientError(Http.RequestHeader request, int statusCode, String message) {
        if(statusCode == Http.Status.REQUEST_ENTITY_TOO_LARGE) {
            String error = "This file is too large";
            Http.Context.current().flash().put(BaseController.KEY_ERROR, error);
            return CompletableFuture.completedFuture(redirect(request.path()));
        } else if (statusCode == Http.Status.NOT_FOUND) {
            return CompletableFuture.completedFuture(notFound(NotFoundPage.render()));
        } else {
            return super.onClientError(request, statusCode, message);
        }
    }

}
