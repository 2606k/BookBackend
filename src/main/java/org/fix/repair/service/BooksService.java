package org.fix.repair.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.fix.repair.entity.books;

public interface BooksService extends IService<books> {
    books getBook(Long bookId);
}
