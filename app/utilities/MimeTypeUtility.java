https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package utilities;

/**
 *
 */
public class MimeTypeUtility {

    public static final String[] ACCEPTED_MIME_TYPES =
            {"image/jpeg", "image/jpg", "image/gif", "iamge/x-ms-bmp", "image/png"};

    public static boolean isMimeTypeAccepted(String mimeType) {
        for (String acceptedMimeType : ACCEPTED_MIME_TYPES) {
            if (acceptedMimeType.equals(mimeType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return The acceptable file extensions for this server.
     */
    public static String getAcceptedFileExtensions() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ACCEPTED_MIME_TYPES.length; i++) {
            builder.append(getFileExtensionFromMimeType(ACCEPTED_MIME_TYPES[i]));
            if (i < ACCEPTED_MIME_TYPES.length - 2) {
                builder.append(", ");
            } else if (i == ACCEPTED_MIME_TYPES.length - 2) {
                builder.append(" or ");
            }
        }
        return builder.toString();
    }

    public static String getFileExtensionFromMimeType(String mimeType) {
        if (ACCEPTED_MIME_TYPES[0].equals(mimeType)) {
            return ".jpeg";
        } else if (ACCEPTED_MIME_TYPES[1].equals(mimeType)) {
            return ".jpg";
        } else if (ACCEPTED_MIME_TYPES[2].equals(mimeType)) {
            return ".gif";
        } else if (ACCEPTED_MIME_TYPES[3].equals(mimeType)) {
            return ".bmp";
        } else if (ACCEPTED_MIME_TYPES[4].equals(mimeType)) {
            return ".png";
        } else {
            return "";
        }
    }

}
