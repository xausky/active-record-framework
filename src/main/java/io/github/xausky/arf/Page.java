package io.github.xausky.arf;

import java.util.Collections;
import java.util.List;

/**
 * Created by xausky on 11/14/16.
 */
public class Page<T extends Model> {
    private List<T> list = Collections.EMPTY_LIST;
    private int page = 0;
    private int size = 0;
    private int totalPage = 0;
    private int totalSize = 0;
    public Page(){

    }

    public Page(int page, int size){
        this.page = page;
        this.size = size;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(int totalPage) {
        this.totalPage = totalPage;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }
}
