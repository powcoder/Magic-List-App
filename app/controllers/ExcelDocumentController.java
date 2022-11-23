https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package controllers;

import excel.ExcelDocumentCreator;
import global.authentication.SubscriptionAuthenticator;
import model.ControllerComponents;
import model.prospect.Prospect;
import play.mvc.Result;
import play.mvc.Security;
import utilities.ResultUtility;
import utilities.Validation;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Corey on 3/10/2017.
 * Project: Magic List Maker - Server
 * <p></p>
 * Purpose of Class:
 */
public class ExcelDocumentController extends BaseController {

    private static final String KEY_FILE_NAME = "file_name";

    @Inject
    public ExcelDocumentController(ControllerComponents controllerComponents) {
        super(controllerComponents);
    }

    @Security.Authenticated(SubscriptionAuthenticator.class)
    public Result getExcelFileForPersonList() {
        String filename = Validation.string(KEY_FILE_NAME, request().queryString());
        if (filename == null) {
            return badRequest(ResultUtility.getNodeForMissingField(KEY_FILE_NAME));
        }

        // TODO get person list
        List<Prospect> prospectList = new ArrayList<>();
        ExcelDocumentCreator.ExcelFile excelFile = new ExcelDocumentCreator(filename)
                .createSheet(prospectList);
        return ok(excelFile.getFileBytes())
                .withHeaders(CONTENT_DISPOSITION, "inline; filename=\"" + excelFile.getFilename() + "\"");
    }

}
