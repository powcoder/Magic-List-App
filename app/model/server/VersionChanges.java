https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.server;

/**
 * Created by Corey on 8/14/2017.
 * Project: magic_list_maker-server
 * <p></p>
 * Purpose of Class:
 */
public class VersionChanges {

    private final int version;
    private final String versionChanges;
    private final long releaseDate;
    private final boolean isDismissed;

    public VersionChanges(int version, String versionChanges, long releaseDate, boolean isDismissed) {
        this.version = version;
        this.versionChanges = versionChanges;
        this.releaseDate = releaseDate;
        this.isDismissed = isDismissed;
    }

    public int getVersion() {
        return version;
    }

    public String getVersionChanges() {
        return versionChanges;
    }

    public long getReleaseDate() {
        return releaseDate;
    }

    public boolean isDismissed() {
        return isDismissed;
    }
}
