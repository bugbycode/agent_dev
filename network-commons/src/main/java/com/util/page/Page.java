package com.util.page;

import java.io.Serializable;

public class Page implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7871685419664199797L;
	
	public static final int DEFAULT_PAGE_SIZE_ONETEN = 10;
	public static final int DEFAULT_PAGE_SIZE = 15;
	public static final int DEFAULT_PAGE_SIZE_THREE = 30;
	public static final int DEFAULT_PAGE_SIZE_FIVE = 50;
	public static final int DEFAULT_PAGE_SIZE_TEN = 100;
	public static final int DEFAULT_PAGE_SIZE_THRETY = 300;
	public static final int DEFAULT_PAGE_SIZE_FIVETY = 500;

	public static final int DEFAULT_RULE_PAGE_SIZE = 50;

	private int pageSize = 15;

	private int totalCount; // 所有条数

	private int startIndex = 0; // 起始页

	private int[] indexes = new int[0];

	public Page(int pageSize, int startIndex) {
		this.pageSize = pageSize;
		this.startIndex = startIndex;
	}

	public void setTotalCount(int totalCount) {
		if (totalCount > 0) {
			this.totalCount = totalCount;
			int count = totalCount / pageSize;
			if (totalCount % pageSize > 0)
				count++;
			indexes = new int[count];
			for (int i = 0; i < count; i++) {
				indexes[i] = pageSize * i;
			}
		} else {
			this.totalCount = 0;
		}
	}

	public int getTotalCount() {
		return totalCount;
	}

	public void setIndexes(int[] indexes) {
		this.indexes = indexes;
	}

	public int[] getIndexes() {
		return indexes;
	}

	public void setStartIndex(int startIndex) {
		if (totalCount <= 0)
			this.startIndex = 0;
		else if (startIndex >= totalCount)
			this.startIndex = indexes[indexes.length - 1];
		else if (startIndex < 0)
			this.startIndex = 0;
		else {
			this.startIndex = indexes[startIndex / pageSize];
		}
	}

	public int getStartIndex() {

		return startIndex;
	}

	public int getNextIndex() {
		int nextIndex = getStartIndex() + pageSize;
		if (nextIndex >= totalCount)
			return getStartIndex();
		else
			return nextIndex;
	}

	public int getPreviousIndex() {
		int previousIndex = this.getStartIndex() - this.pageSize;

		if (previousIndex <= 0)
			return 0;
		else
			return previousIndex;
	}

	public int getPageCount() {
		int count = totalCount / pageSize;
		if (totalCount % pageSize > 0)
			count++;
		return count;
	}

	public int getCurrentPage() {
		if (0 == this.getPageCount()) {
			return 0;
		} else {
			return getStartIndex() / pageSize + 1;
		}
	}

	public int getLastIndex() {

		if (indexes.length <= 0) {
			return 0;
		} else {
			return indexes[indexes.length - 1];
		}
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}
	
	
}