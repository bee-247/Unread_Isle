package com.tongji.knowpost.service;

import com.tongji.knowpost.api.dto.FeedPageResponse;

/**
 * 知文 Feed 业务接口。
 */
public interface KnowPostFeedService {
    FeedPageResponse getPublicFeed(int page, int size, Long currentUserIdNullable);

    FeedPageResponse getMyPublished(long userId, int page, int size);

    /** 发布后让公共与作者个人 Feed 立即切换缓存版本。 */
    void invalidateFeedCaches(long userId);
}
