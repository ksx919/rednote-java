-- 用户表
CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `email` VARCHAR(128) NOT NULL COMMENT '邮箱',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '加密后的密码',
  `nickname` VARCHAR(64) NOT NULL DEFAULT '.' COMMENT '昵称',
  `avatar_url` VARCHAR(512) DEFAULT '' COMMENT '头像链接',
  `bio` VARCHAR(255) DEFAULT '暂时还没有简介' COMMENT '个人简介(小红书的签名)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_email` (`email`) USING BTREE COMMENT '邮箱必须唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 帖子/笔记表
CREATE TABLE IF NOT EXISTS `posts` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '作者ID',
  `title` VARCHAR(100) NOT NULL DEFAULT '' COMMENT '标题(小红书标题通常较短)',
  `content` TEXT COMMENT '正文内容',
  `images` JSON COMMENT '图片URL集合，JSON数组格式 ["url1", "url2"]',
  `like_count` INT UNSIGNED NOT NULL DEFAULT '0' COMMENT '点赞数',
  `comment_count` INT UNSIGNED NOT NULL DEFAULT '0' COMMENT '评论数',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`) USING BTREE COMMENT '用于查找某人的所有帖子',
  KEY `idx_created_at` (`created_at`) USING BTREE COMMENT '用于首页按时间流推荐'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子/笔记表';

-- 评论表
CREATE TABLE IF NOT EXISTS `comments` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `post_id` BIGINT UNSIGNED NOT NULL COMMENT '所属帖子ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '评论者ID',
  `content` VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '评论内容',
  `like_count` INT UNSIGNED NOT NULL DEFAULT '0' COMMENT '点赞数',
  
  -- 核心：无限级评论的实现字段
  `parent_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '直接父评论ID (回复了谁)',
  `root_parent_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '顶级根评论ID (为了快速聚合楼中楼)',
  `reply_to_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '被回复的用户ID (方便前端显示 "回复 @某某")',

  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`) USING BTREE COMMENT '查询某帖子的评论',
  KEY `idx_root_parent` (`root_parent_id`) USING BTREE COMMENT '查询某根评论下的所有回复'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 补充评论表字段 (图片支持)
-- 注意：如果表是新创建的，建议直接合并到 CREATE TABLE 中。这里保留 ALTER 语句以兼容旧表结构升级。
-- 检查列是否存在通常需要存储过程，这里直接尝试添加，如果已存在可能会报错，建议在全新环境运行。
ALTER TABLE `comments`
ADD COLUMN `image_url` VARCHAR(512) DEFAULT NULL COMMENT '评论图片URL (若为空则是纯文本)' AFTER `content`,
ADD COLUMN `image_width` INT UNSIGNED DEFAULT 0 COMMENT '图片宽度' AFTER `image_url`,
ADD COLUMN `image_height` INT UNSIGNED DEFAULT 0 COMMENT '图片高度' AFTER `image_width`;

-- 补充帖子表字段 (封面尺寸)
ALTER TABLE `posts` 
ADD COLUMN `img_width` INT UNSIGNED DEFAULT 0 COMMENT '封面图/首图宽度(px)' AFTER `images`,
ADD COLUMN `img_height` INT UNSIGNED DEFAULT 0 COMMENT '封面图/首图高度(px)' AFTER `img_width`;


-- 帖子点赞记录表
CREATE TABLE IF NOT EXISTS `post_likes` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '点赞的用户ID',
  `post_id` BIGINT UNSIGNED NOT NULL COMMENT '被点赞的帖子ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  -- 核心：联合唯一索引。保证一个用户对同一个帖子只能点赞一次
  UNIQUE KEY `uk_user_post` (`user_id`, `post_id`), 
  KEY `idx_post_id` (`post_id`) -- 用于查询某帖子被谁点赞过
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子点赞记录表';

-- 评论点赞记录表
CREATE TABLE IF NOT EXISTS `comment_likes` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '点赞的用户ID',
  `comment_id` BIGINT UNSIGNED NOT NULL COMMENT '被点赞的评论ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  -- 核心：联合唯一索引。保证一个用户对同一个帖子只能点赞一次
  UNIQUE KEY `uk_user_comment` (`user_id`, `comment_id`), 
  KEY `idx_comment_id` (`comment_id`) -- 用于查询某评论被谁点赞过
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞记录表';

-- ==========================================
-- 即时通讯相关表
-- ==========================================

-- 会话表（支持单聊和群聊）
CREATE TABLE IF NOT EXISTS `conversations` (
  `id` VARCHAR(64) NOT NULL COMMENT '会话ID，格式：单聊为 user1_user2（小ID在前），群聊为 group_timestamp',
  `type` VARCHAR(20) NOT NULL DEFAULT 'PRIVATE' COMMENT '会话类型：PRIVATE(单聊), GROUP(群聊)',
  `name` VARCHAR(100) DEFAULT NULL COMMENT '会话名称（群聊必填，单聊为空）',
  `avatar` VARCHAR(512) DEFAULT NULL COMMENT '会话头像（群聊可设置，单聊为空）',
  `creator_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建者ID（群聊有效）',
  `member_count` INT UNSIGNED DEFAULT 2 COMMENT '成员数量',

  -- 最后消息信息（冗余字段，提升查询性能）
  `last_message_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '最后一条消息ID',
  `last_message_content` VARCHAR(500) DEFAULT '' COMMENT '最后一条消息内容',
  `last_message_time` DATETIME DEFAULT NULL COMMENT '最后消息时间',
  `last_sender_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '最后发送者ID',

  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`),
  KEY `idx_creator` (`creator_id`),
  KEY `idx_last_time` (`last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表（支持单聊和群聊）';

-- 会话成员表
CREATE TABLE IF NOT EXISTS `conversation_members` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `conversation_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',

  -- 成员角色（群聊有效）
  `role` VARCHAR(20) DEFAULT 'MEMBER' COMMENT '角色：OWNER(群主), ADMIN(管理员), MEMBER(普通成员)',

  -- 用户个性化设置
  `unread_count` INT UNSIGNED DEFAULT 0 COMMENT '未读消息数',
  `last_read_message_id` BIGINT UNSIGNED DEFAULT 0 COMMENT '最后已读消息ID',
  `is_muted` TINYINT(1) DEFAULT 0 COMMENT '是否免打扰',
  `is_pinned` TINYINT(1) DEFAULT 0 COMMENT '是否置顶',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除会话（用户侧）',

  `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `left_at` DATETIME DEFAULT NULL COMMENT '退出时间（退群时设置）',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_user` (`conversation_id`, `user_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_conversation` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话成员表';

-- 消息表
CREATE TABLE IF NOT EXISTS `messages` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `conversation_id` VARCHAR(64) NOT NULL COMMENT '所属会话ID',
  `sender_id` BIGINT UNSIGNED NOT NULL COMMENT '发送者ID',

  -- 消息内容
  `type` VARCHAR(20) NOT NULL DEFAULT 'TEXT' COMMENT '消息类型：TEXT(文本), IMAGE(图片), VIDEO(视频), AUDIO(语音), SYSTEM(系统消息)',
  `content` TEXT NOT NULL COMMENT '消息内容（文本消息直接存储，其他类型存储描述）',
  `extra_data` JSON DEFAULT NULL COMMENT '扩展数据（图片URL、视频URL、语音时长等）',

  -- 引用消息（回复功能）
  `reply_to_message_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '回复的消息ID',

  -- 撤回相关
  `is_recalled` TINYINT(1) DEFAULT 0 COMMENT '是否已撤回',
  `recalled_at` DATETIME DEFAULT NULL COMMENT '撤回时间',

  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',

  PRIMARY KEY (`id`),
  KEY `idx_conversation_time` (`conversation_id`, `created_at`),
  KEY `idx_sender` (`sender_id`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';
