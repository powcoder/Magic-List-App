https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import database.ClientRevisionDBAccessor;
import com.typesafe.config.Config;
import model.ControllerComponents;
import play.Environment;
import play.cache.SyncCacheApi;
import play.db.Database;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.mvc.Result;
import utilities.ResultUtility;
import utilities.Validation;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 *
 */
public class ClientRevisionController extends BaseController {

    private static final String DROPBOX_API_ARG = "Dropbox-API-Arg";
    private final String DROP_BOX_ACCESS_TOKEN;

    private static final String DROP_BOX_DOWNLOAD_URL = "https://content.dropboxapi.com/2/files/download";

    @Inject
    public ClientRevisionController(ControllerComponents controllerComponents) {
        super(controllerComponents);

        DROP_BOX_ACCESS_TOKEN = getString("DROP_BOX_ACCESS_TOKEN");
    }

    public Result getWindowsVersionNumber() {
        double currentVersion = new ClientRevisionDBAccessor(getDatabase())
                .getWindowsCurrentVersion();
        if (currentVersion == -1) {
            return internalServerError(ResultUtility.getNodeForBooleanResponse(false));
        } else {
            ObjectNode objectNode = Json.newObject()
                    .put("version", currentVersion);
            return ok(objectNode);
        }
    }

    public Result getMacVersionNumber() {
        double currentVersion = new ClientRevisionDBAccessor(getDatabase())
                .getMacCurrentVersion();
        if (currentVersion == -1) {
            return internalServerError(ResultUtility.getNodeForBooleanResponse(false));
        } else {
            ObjectNode objectNode = Json.newObject()
                    .put("version", currentVersion);
            return ok(objectNode);
        }
    }

    public CompletionStage<Result> getMacClient() {
        return getClient("Importer.tar.gz");
    }

    public CompletionStage<Result> getWindowsClient() {
        return getClient("Importer.zip");
    }

    private CompletionStage<Result> getClient(String filename) {
        return getWsClient().url(DROP_BOX_DOWNLOAD_URL)
                .addHeader(AUTHORIZATION, "Bearer " + DROP_BOX_ACCESS_TOKEN)
                .addHeader(DROPBOX_API_ARG, "{\"path\": \"/" + filename + "\"}")
                .setMethod("POST")
                .stream()
                .thenApply(streamedResponse -> {
                    String fileSize = Validation.string(CONTENT_LENGTH, streamedResponse.getHeaders());

                    Result result = ok().chunked(streamedResponse.getBodyAsSource());

                    if (fileSize != null) {
                        result = result.withHeader(CONTENT_LENGTH, fileSize);
                    }

                    return result.withHeaders(CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
                });
    }

}
