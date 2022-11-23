https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package global;

import play.filters.cors.CORSFilter;
import play.filters.csrf.CSRFFilter;
import play.filters.gzip.GzipFilter;
import play.filters.headers.SecurityHeadersFilter;
import play.http.DefaultHttpFilters;

import javax.inject.Inject;

public class AppFilters extends DefaultHttpFilters {

    @Inject
    public AppFilters(SslFilter sslFilter, CSRFFilter csrfFilter, CORSFilter corsFilter,
                      SecurityHeadersFilter securityHeadersFilter, GzipFilter gzipFilter) {
        super(sslFilter, csrfFilter, corsFilter, securityHeadersFilter, gzipFilter);
    }

}
