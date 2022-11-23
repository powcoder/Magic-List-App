https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.oauth;

public enum OAuthProvider {

   OUTLOOK, INVALID;

   private static final String KEY_OUTLOOK = "outlook";

   public static OAuthProvider parse(String text) {
       if(KEY_OUTLOOK.equalsIgnoreCase(text)) {
           return OUTLOOK;
       } else {
           return INVALID;
       }
   }

    @Override
    public String toString() {
        if(this == OUTLOOK) {
            return "Microsoft Outlook";
        } else {
            try {
                throw new IllegalAccessException("Invalid argument, found: " + this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "Invalid";
        }
    }

    public String getRawText() {
        if(this == OUTLOOK) {
            return KEY_OUTLOOK;
        } else {
            try {
                throw new IllegalAccessException("Invalid argument, found: " + this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "invalid";
        }
    }

}
