https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import model.prospect.Notification;
import play.twirl.api.Html;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("unused")
public class PagedList<T> extends ArrayList<T> {

    public static class Wrapper {

        private final List<?> list;
        private final int minRange;
        private final int maxRange;
        private final int totalNumberOfItems;

        public Wrapper(PagedList<?> pagedList) {
            this.list = pagedList;
            minRange = pagedList.isEmpty() ? 0 : (pagedList.currentPage - 1) * pagedList.numberOfItemsPerPage + 1;
            maxRange = pagedList.isEmpty() ? 0 : (pagedList.currentPage - 1) * pagedList.numberOfItemsPerPage + pagedList.size();

            totalNumberOfItems = pagedList.getTotalNumberOfItems();
        }

    }

    private static final long serialVersionUID = 8675309L;

    private int currentPage;
    private int maxPage;
    private int numberOfItemsPerPage;
    private int totalNumberOfItems;
    private boolean isMaxPageSet = false;

    public PagedList(int currentPage, int numberOfItemsPerPage) {
        super();
        init(currentPage, numberOfItemsPerPage);
    }

    public PagedList(int initialCapacity, int currentPage, int numberOfItemsPerPage) {
        super(initialCapacity);
        init(currentPage, numberOfItemsPerPage);
    }

    public PagedList(Collection<? extends T> collection, int currentPage, int numberOfItemsPerPage) {
        super(collection);
        init(currentPage, numberOfItemsPerPage);
    }

    private void init(int page, int pageSize) {
        this.currentPage = page;
        this.numberOfItemsPerPage = pageSize;
        this.maxPage = 1;
        this.totalNumberOfItems = 0;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getMaxPage() {
        return maxPage;
    }

    public void setMaxPage(int maxPage) {
        this.maxPage = maxPage;
    }

    public int getNumberOfItemsPerPage() {
        return numberOfItemsPerPage;
    }

    public String getTotalNumberOfItemsForUI() {
        return NumberFormat.getInstance().format(totalNumberOfItems);
    }

    public Html getHtmlRangeForUi() {
        return getHtmlRangeForUi(null);
    }

    public Html getHtmlRangeForUi(String id) {
        int currentPageStartRange = this.isEmpty() ? 0 : (currentPage - 1) * numberOfItemsPerPage + 1;
        int currentPageEndRange = this.isEmpty() ? 0 : (currentPage - 1) * numberOfItemsPerPage + this.size();
        if (id != null) {
            // language=HTML
            return Html.apply(
                    "Showing <span id='page_pagination_min-" + id + "'>" + (currentPageStartRange) + "</span> - " +
                            "<span id='page_pagination_max-" + id + "'>" + (currentPageEndRange) + "</span> " +
                            "of " +
                            "<span id='page_pagination_total-" + id + "'>" + totalNumberOfItems + "</span>"
            );

        } else {
            // language=HTML
            return Html.apply(
                    "Showing <span id='page_pagination_min'>" + (currentPageStartRange) + "</span> - " +
                            "<span id='page_pagination_max'>" + (currentPageEndRange) + "</span> " +
                            "of " +
                            "<span id='page_pagination_total'>" + totalNumberOfItems + "</span>"
            );
        }
    }

    public void setTotalNumberOfItems(int totalNumberOfItems) {
        this.isMaxPageSet = true;
        this.totalNumberOfItems = totalNumberOfItems;

        if (totalNumberOfItems % numberOfItemsPerPage == 0 && totalNumberOfItems > 0) {
            maxPage = totalNumberOfItems / numberOfItemsPerPage;
        } else {
            maxPage = (totalNumberOfItems / numberOfItemsPerPage) + 1;
        }
    }

    public int getTotalNumberOfItems() {
        return totalNumberOfItems;
    }

    public boolean isMaxPageSet() {
        return isMaxPageSet;
    }

    public ObjectNode renderAsObject() {
        return null;
    }
}
