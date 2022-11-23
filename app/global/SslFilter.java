https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package global;

import akka.stream.Materializer;
import play.Logger;
import play.api.Play;
import play.mvc.Filter;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import utilities.Validation;

import javax.inject.Inject;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

class SslFilter extends Filter {

    private final Logger.ALogger logger = Logger.of(this.getClass());

    @Inject
    public SslFilter(Materializer materializer) {
        super(materializer);
    }

    @Override
    public CompletionStage<Result> apply(Function<Http.RequestHeader, CompletionStage<Result>> requestHeaderFunction,
                                         Http.RequestHeader requestHeader) {
        String subDomain = "";
        try {
            subDomain = requestHeader.host().split("[.]")[0];
        } catch (Exception e) {
            logger.error("Could not parse sub-domain: ", e);
        }

        if (requestHeader.host().equalsIgnoreCase(subDomain)) {
            subDomain = "www";
        } else {
            subDomain = "";
        }

        if (Play.current().isProd()) {
            List<String> httpsHeader = requestHeader.getHeaders()
                    .toMap()
                    .getOrDefault(Http.HeaderNames.X_FORWARDED_PROTO, Collections.singletonList("http"));

            if (Validation.isEmpty(httpsHeader.get(0)) ||
                    httpsHeader.get(0).equalsIgnoreCase("http")) {

                return CompletableFuture.completedFuture(
                        Results.movedPermanently("https://".concat(subDomain).concat(requestHeader.host()).concat(requestHeader.uri()))
                );
            }
        }

        return requestHeaderFunction.apply(requestHeader).toCompletableFuture();
    }

}
