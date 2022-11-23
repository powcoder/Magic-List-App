https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package global;

import controllers.BaseController;
import model.account.Account;
import play.Logger;
import play.http.DefaultActionCreator;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import utilities.Validation;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 *
 */
public class HttpDefaultResponseActionCreator extends DefaultActionCreator {

    @Override
    public Action createAction(Http.Request request, Method actionMethod) {
        return new Action.Simple() {
            @Override
            public CompletionStage<Result> call(Http.Context context) {
                Http.Response response = context.response();
                response.setHeader(BaseController.CACHE_CONTROL, "no-cache, max-age=0, must-revalidate, no-store");
                response.setHeader(BaseController.EXPIRES, "0");
                response.setHeader(BaseController.PRAGMA, "no-cache");

                return delegate.call(context)
                        .thenApply(result -> {
                            result.header(BaseController.CACHE_CONTROL)
                                    .ifPresent(s -> response.setHeader(BaseController.CACHE_CONTROL, s));
                            return result;
                        });
            }
        };

    }

}
