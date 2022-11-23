https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package utilities;

import play.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 *
 */
public final class FutureUtility {

    private static final Logger.ALogger logger = Logger.of(FutureUtility.class);

    public static <T> T getFromFutureQuietly(CompletionStage<T> stage) {
        try {
            return stage.toCompletableFuture()
                    .get(30, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            logger.error("Error: ", e);
            return null;
        }
    }

    /**
     * @param stages The list of completion stages that will all return lists.
     * @return NULL if any of the stages returns a null list, a list containing all of the collections added together
     * otherwise
     */
    public static <T> CompletableFuture<List<T>> combineStages(List<CompletableFuture<List<T>>> stages) {
        return CompletableFuture.allOf(stages.toArray(new CompletableFuture[stages.size()]))
                .thenApply(aVoid -> {
                    List<T> list = new ArrayList<>();
                    for (CompletionStage<List<T>> stage : stages) {
                        List<T> collection = stage.toCompletableFuture().join();
                        if (collection != null) {
                            list.addAll(collection);
                        } else {
                            return null;
                        }
                    }
                    return list;
                });
    }

}
