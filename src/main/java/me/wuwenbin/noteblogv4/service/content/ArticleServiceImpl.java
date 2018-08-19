package me.wuwenbin.noteblogv4.service.content;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import me.wuwenbin.noteblogv4.dao.mapper.ArticleMapper;
import me.wuwenbin.noteblogv4.dao.repository.ArticleRepository;
import me.wuwenbin.noteblogv4.dao.repository.TagReferRepository;
import me.wuwenbin.noteblogv4.dao.repository.TagRepository;
import me.wuwenbin.noteblogv4.model.constant.NoteBlogV4;
import me.wuwenbin.noteblogv4.model.constant.TagType;
import me.wuwenbin.noteblogv4.model.entity.NBArticle;
import me.wuwenbin.noteblogv4.model.entity.NBTag;
import me.wuwenbin.noteblogv4.model.entity.NBTagRefer;
import me.wuwenbin.noteblogv4.model.pojo.framework.Pagination;
import me.wuwenbin.noteblogv4.model.pojo.vo.NBArticleVO;
import me.wuwenbin.noteblogv4.service.param.ParamService;
import me.wuwenbin.noteblogv4.util.NBUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.transaction.Transactional;

import static cn.hutool.core.util.RandomUtil.randomInt;
import static java.time.LocalDateTime.now;

/**
 * created by Wuwenbin on 2018/8/5 at 20:09
 *
 * @author wuwenbin
 */
@Slf4j
@Service
public class ArticleServiceImpl implements ArticleService {

    private final ArticleRepository articleRepository;
    private final TagReferRepository tagReferRepository;
    private final TagRepository tagRepository;
    private final ArticleMapper articleMapper;

    @Autowired
    public ArticleServiceImpl(ArticleRepository articleRepository, TagReferRepository tagReferRepository, TagRepository tagRepository, ArticleMapper articleMapper) {
        this.articleRepository = articleRepository;
        this.tagReferRepository = tagReferRepository;
        this.tagRepository = tagRepository;
        this.articleMapper = articleMapper;
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public void createArticle(NBArticle article, String tagNames) {
        if (StringUtils.isEmpty(tagNames)) {
            throw new RuntimeException("tagNames 不能为空！");
        }
        if (!StringUtils.isEmpty(article.getUrlSequence())) {
            boolean isExistUrl = articleRepository.countByUrlSequence(article.getUrlSequence()) > 0;
            if (isExistUrl) {
                throw new RuntimeException("已存在 url：" + article.getUrlSequence());
            }
        }
        setArticleSummaryAndTxt(article);
        decorateArticle(article);
        NBArticle newArticle = articleRepository.save(article);
        String[] tagNameArray = tagNames.split(",");
        saveTags(newArticle, tagNameArray, tagRepository, tagReferRepository);
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public void updateArticle(NBArticle article, String tagNames) {
        if (StringUtils.isEmpty(article.getId())) {
            throw new RuntimeException("未指定修改文章的ID");
        }
        if (StringUtils.isEmpty(tagNames)) {
            throw new RuntimeException("tagNames 不能为空！");
        }
        if (!StringUtils.isEmpty(article.getUrlSequence())) {
            boolean isExistUrl = articleRepository.countByUrlSequence(article.getUrlSequence()) > 0;
            if (isExistUrl) {
                throw new RuntimeException("已存在 url：" + article.getUrlSequence());
            }
        }
        setArticleSummaryAndTxt(article);
        decorateArticle(article);
        NBArticle updateArticle = articleRepository.save(article);
        if (updateArticle != null) {
            tagReferRepository.deleteByReferId(updateArticle.getId());
            String[] tagNameArray = tagNames.split(",");
            saveTags(updateArticle, tagNameArray, tagRepository, tagReferRepository);
        }
    }

    @Override
    public Page<NBArticleVO> findPageInfo(Pagination<NBArticleVO> articlePage, String title, Long authorId) {
        PageHelper.startPage(articlePage.getPage(), articlePage.getLimit(), articlePage.getOrderBy());
        return articleMapper.findPageInfo(articlePage, title, authorId);
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public boolean updateTopById(long articleId, boolean top) {
        if (top) {
            int maxTop = articleMapper.findMaxTop();
            return articleRepository.updateTopById(maxTop + 1, articleId) == 1;
        } else {
            int currentTop = articleRepository.getOne(articleId).getTop();
            articleRepository.updateTopsByTop(currentTop);
            return articleRepository.updateTopById(0, articleId) == 1;
        }
    }

    /**
     * 根据文章内容生成文章摘要
     *
     * @param article
     */
    private static void setArticleSummaryAndTxt(NBArticle article) {
        ParamService paramService = NBUtils.getBean(ParamService.class);
        int summaryLength = Integer.valueOf(paramService.getValueByName(NoteBlogV4.Param.ARTICLE_SUMMARY_WORDS_LENGTH));
        String clearContent = HtmlUtil.cleanHtmlTag(StrUtil.trim(article.getContent()));
        clearContent = StringUtils.trimAllWhitespace(clearContent);
        clearContent = clearContent.substring(0, clearContent.length() < summaryLength ? clearContent.length() : summaryLength);
        int allStandardLength = clearContent.length();
        int fullAngelLength = NBUtils.fullAngelWords(clearContent);
        int finalLength = allStandardLength - fullAngelLength / 2;
        if (StringUtils.isEmpty(article.getSummary())) {
            article.setSummary(clearContent.substring(0, finalLength < summaryLength ? finalLength : summaryLength));
        }
        article.setTextContent(clearContent);
    }

    /**
     * 装饰article的一些空值为默认值
     *
     * @param article
     */
    private static void decorateArticle(NBArticle article) {
        article.setPost(now());
        article.setView(randomInt(666, 1609));
        article.setApproveCnt(randomInt(6, 169));
        if (StringUtils.isEmpty(article.getAppreciable())) {
            article.setAppreciable(false);
        }
        if (StringUtils.isEmpty(article.getCommented())) {
            article.setCommented(false);
        }
        if (StringUtils.isEmpty(article.getTop())) {
            article.setTop(0);
        }
    }

    /**
     * 保存文章的 tags
     *
     * @param updateArticle
     * @param tagNameArray
     * @param tagRepository
     * @param tagReferRepository
     */
    private static void saveTags(NBArticle updateArticle, String[] tagNameArray , TagRepository tagRepository, TagReferRepository tagReferRepository) {
        int cnt = 0;
        for (String name : tagNameArray) {
            Example<NBTag> condition = Example.of(NBTag.builder().name(name).build());
            boolean isExist = tagRepository.count(condition) == 0;
            long tagId = isExist ?
                    tagRepository.save(NBTag.builder().name(name).build()).getId() :
                    tagRepository.findByName(name).getId();

            tagReferRepository.save(
                    NBTagRefer.builder()
                            .referId(updateArticle.getId())
                            .tagId(tagId)
                            .show(cnt < 4)
                            .type(TagType.article.name()).build()
            );
            cnt++;
        }
    }
}
