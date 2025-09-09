package org.fix.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.fix.repair.entity.books;
import org.fix.repair.mapper.BooksMapper;
import org.fix.repair.service.BooksService;
import org.springframework.stereotype.Service;

@Service
public class BooksServiceImpl extends ServiceImpl<BooksMapper, books> implements BooksService {
    @Override
    public books getBook(Long bookId) {
        LambdaQueryWrapper<books> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(books::getId, bookId);
        books book = this.getOne(wrapper);
        return book;
    }

}
