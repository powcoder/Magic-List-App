https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package clients;

/**
 * Created by Corey on 8/3/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class SubscriptionAssetsClient extends GoogleCloudStorageClient {

    private static final String BUCKET_NAME = "subscription-assets";
    private static final String CREDENTIAL_FILE = "conf/magic-list-cloud-storage.json";

    public SubscriptionAssetsClient() {
        super(CREDENTIAL_FILE, BUCKET_NAME);
    }

}
