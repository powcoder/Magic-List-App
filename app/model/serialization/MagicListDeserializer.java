https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model.serialization;

import com.google.gson.*;
import model.ImporterLimit;
import model.PagedList;
import model.account.*;
import model.calendar.BaseCalendarTemplate;
import model.calendar.CalendarEvent;
import model.calendar.CalendarProvider;
import model.dialsheet.*;
import model.graph.*;
import model.manager.*;
import model.oauth.OAuthAccount;
import model.oauth.OAuthProvider;
import model.oauth.OAuthToken;
import model.oauth.OAuthTokenAccountWrapper;
import model.outlook.*;
import model.profile.PersonStatusQuickLink;
import model.prospect.ContactStatusAuditItem;
import model.prospect.Notification;
import model.prospect.Prospect;
import model.prospect.ProspectState;
import model.lists.ProspectSearch;
import model.lists.SavedList;
import model.stripe.CvcCheck;
import model.stripe.Plan;
import model.user.BugReport;
import model.user.Suggestion;
import model.user.User;
import model.user.UserQuote;
import utilities.RandomStringGenerator;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Corey Caplan on 9/4/17.
 */
final class MagicListDeserializer {

    static class ProspectInstanceCreator implements InstanceCreator<Prospect> {

        @Override
        public Prospect createInstance(Type type) {
            return Prospect.Factory.createFromId(RandomStringGenerator.getInstance().getNextRandomPersonId());
        }

    }

    static class PagedListSerializer implements JsonSerializer<PagedList> {

        @Override
        public JsonElement serialize(PagedList src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(new PagedList.Wrapper(src));
        }

    }

}
